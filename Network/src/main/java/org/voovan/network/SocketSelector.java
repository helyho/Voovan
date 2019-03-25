package org.voovan.network;

import org.voovan.network.tcp.TcpServerSocket;
import org.voovan.network.udp.UdpServerSocket;
import org.voovan.network.udp.UdpSession;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.*;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 * 选择器
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SocketSelector implements Closeable {
	public static final int IO_LOOP_WAIT_TIME = Integer.valueOf(TObject.nullDefault(System.getProperty("IoLoopWaitTime"),"100"));
	static {
		System.out.println("[SOCKET] IO_LOOP_WAIT_TIME: " + IO_LOOP_WAIT_TIME + "ms");
	}

	private  EventRunner eventRunner;

	protected Selector selector;
	protected ByteBuffer readTempBuffer;

	protected SimpleArraySet<SelectionKey> selectionKeys = new SimpleArraySet<SelectionKey>(1024);

	/**
	 * 构造方法
	 * @param eventRunner 事件执行器
	 * @throws IOException IO 异常
	 */
	public SocketSelector(EventRunner eventRunner) throws IOException {
		this.selector = SelectorProvider.provider().openSelector();
		this.eventRunner = eventRunner;

		readTempBuffer = TByteBuffer.allocateDirect();

		try {
			TReflect.setFieldValue(selector, NioUtil.selectedKeysField, selectionKeys);
			TReflect.setFieldValue(selector, NioUtil.publicSelectedKeysField, selectionKeys);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}

		//首次触发事件循环
		addChooseEvent();
	}

	public EventRunner getEventRunner() {
		return eventRunner;
	}

	/**
	 * 注册一个 SocketContext 到选择器
	 * @param socketContext SocketContext 对象
	 * @param ops 需要关注的操作
	 * @return true:成功, false:失败
	 */
	public boolean register(SocketContext socketContext, int ops){
		try {
			SelectionKey selectionKey = socketContext.socketChannel().register(selector, ops, socketContext);
			if (socketContext.getSession() != null) {
				socketContext.getSession().setSelectionKey(selectionKey);
				socketContext.getSession().setSocketSelector(this);
			}

			return true;
		} catch (ClosedChannelException e) {
			Logger.error("Register " + socketContext + " to selector error");
			return false;
		}
	}

	/**
	 * 在选择器中取消一个 SocketContext 的注册
	 * @param socketContext SocketContext 对象
	 */
	public void unRegister(SocketContext socketContext) {
		//需要在 cancel 后立刻处理 selectNow
		addChooseEvent(()->{
			socketContext.getSession().getSelectionKey().attach(null);
			socketContext.getSession().getSelectionKey().cancel();
			return true;
		});
	}

	/**
	 * 是否在选择器绑定的执行器的线程中执行
	 * @return
	 */
	private boolean inEventRunner(){
		return eventRunner.getThread() == Thread.currentThread();
	}

	/**
	 * 向执行器中增加一个选择事件
	 */
	public void addChooseEvent(){
		addChooseEvent(null);
	}

	/**
	 * 向执行器中增加一个选择事件
	 * @param supplier 在事件选择前执行的方法
	 */
	public void addChooseEvent(Callable<Boolean> supplier){
		if(selector.isOpen()) {
			eventRunner.addEvent(() -> {
				boolean result = true;
				if(supplier!=null) {
					try {
						result = supplier.call();
					} catch (Exception e) {
						Logger.error("addChoseEvent error:",e);
						result = false;
					}
				}

				if(result) {
					eventChoose();
				}
			});
		}
	}

	int emptyReadyChannelCount = 1;

	/**
	 * 事件选择业务累
	 */
	public void eventChoose() {
		// 事件循环
		try {
			if (selector != null && selector.isOpen()) {
				if (selector.selectNow()>0) {
					emptyReadyChannelCount = 1;

					SimpleArraySet selectionKeys = (SimpleArraySet) selector.selectedKeys();

					for (int i=0;i<selectionKeys.size(); i++) {
						SelectionKey selectionKey = (SelectionKey)selectionKeys.get(i);

						if (selectionKey.isValid()) {
							// 获取 socket 通道
							SelectableChannel channel = selectionKey.channel();
							if (channel.isOpen() && selectionKey.isValid()) {
								// 事件分发,包含时间 onRead onAccept
								// Server接受连接
								if((selectionKey.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
									SocketChannel socketChannel = ((ServerSocketChannel) channel).accept();
									tcpAccept((TcpServerSocket) selectionKey.attachment(), socketChannel);
								}

								// 有数据读取
								if ((selectionKey.readyOps() & SelectionKey.OP_READ) != 0) {
									if(channel instanceof SocketChannel){
										tcpReadFromChannel((TcpSocket) selectionKey.attachment(), (SocketChannel)channel);
									} else if(channel instanceof DatagramChannel) {
										udpReadFromChannel((SocketContext<DatagramChannel, UdpSession>) selectionKey.attachment(), (DatagramChannel) channel);
									}
								}
							}
						} else {
							unRegister((SocketContext) selectionKey.attachment());
						}
					}

					selectionKeys.reset();
				} else {
					//给 OS 切换 EPOLL 中的 fd 的时间, 由于 java 最小只能用 1ms, 实测对性能完全无影响
					TEnv.sleep(emptyReadyChannelCount);
					emptyReadyChannelCount++;
					if(emptyReadyChannelCount > IO_LOOP_WAIT_TIME) {
						emptyReadyChannelCount = 1;
					}
				}
			}
		} catch (IOException e){
			Logger.error("NioSelector error: ", e);
		} finally {
			if(inEventRunner()){
				addChooseEvent();
			}
		}
	}

	/**
	 * 选择器关闭方法
	 */
	public void close() {
		try {
			TByteBuffer.release(readTempBuffer);
			selector.close();
		} catch (IOException e) {
			Logger.error("close selector error");
		}
	}

	/**
	 * 通用封装的从通道读数据的方法
	 * @param socketContext SocketContext 对象
	 * @param selectableChannel 读取的 SelectableChannel 对象
	 * @return 读取数据的字节数, -1:读取失败
	 */
	public int readFromChannel(SocketContext socketContext, SelectableChannel selectableChannel){
		if(selectableChannel instanceof SocketChannel){
			return tcpReadFromChannel((TcpSocket) socketContext, (SocketChannel)selectableChannel);
		} else if(selectableChannel instanceof DatagramChannel){
			return udpReadFromChannel((SocketContext<DatagramChannel, UdpSession>)socketContext, (DatagramChannel) selectableChannel);
		} else {
			return -1;
		}
	}

	/**
	 * 通用封装的向通道写数据的方法
	 * @param socketContext SocketContext 对象
	 * @param buffer 待写入的数据缓冲对象
	 * @return 写入数据的字节数, -1:写入失败
	 */
	public int writeToChannel(SocketContext socketContext, ByteBuffer buffer){
		if(socketContext instanceof TcpSocket){
			return tcpWriteToChannel((TcpSocket) socketContext, buffer);
		} else if(socketContext instanceof UdpSocket){
			return udpWriteToChannel((UdpSocket) socketContext, buffer);
		} else {
			return -1;
		}
	}

	/**
	 * Tcp 服务接受一个新的连接
	 * @param socketContext SocketContext 对象
	 * @param socketChannel Socketchannel 对象
	 */
	public void tcpAccept(TcpServerSocket socketContext, SocketChannel socketChannel) {
		try {
			TcpSocket socket = new TcpSocket(socketContext, socketChannel);
			EventTrigger.fireAccept(socket.getSession());
		} catch(Exception e){
			dealException(socketContext, e);
		}
	}

	/**
	 * TCP 从通道读数据的方法
	 * @param socketContext TcpSocket 对象
	 * @param socketChannel 读取的 Socketchannel 对象
	 * @return 读取数据的字节数, -1:读取失败
	 */
	public int tcpReadFromChannel(TcpSocket socketContext, SocketChannel socketChannel) {
		try {
			int readSize = socketChannel.read(readTempBuffer);
			readSize = loadAndPrepare(socketContext.getSession(), readSize);
			return readSize;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}

	/**
	 * TCP 向通道写数据的方法
	 * @param socketContext TcpSocket 对象
	 * @param buffer 待写入的数据缓冲对象
	 * @return 写入数据的字节数, -1:写入失败
	 */
	public int tcpWriteToChannel(TcpSocket socketContext, ByteBuffer buffer) {
		try {
			int totalSendByte = 0;
			long start = System.currentTimeMillis();
			if (socketContext.isConnected() && buffer != null) {
				//循环发送直到全部内容发送完毕
				while (socketContext.isConnected() && buffer.remaining() != 0) {
					int sendSize = socketContext.socketChannel().write(buffer);
					if (sendSize == 0) {
						TEnv.sleep(1);
						if (System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
							Logger.error("NioSelector tcpWriteToChannel timeout", new TimeoutException());
							socketContext.close();
							return -1;
						}
					} else {
						start = System.currentTimeMillis();
						totalSendByte += sendSize;
					}
				}
			}
			return totalSendByte;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}

	/**
	 * UDP 服务接受一个新的连接
	 * @param socketContext UdpServerSocket 对象
	 * @param datagramChannel DatagramChannel 对象
	 * @param address 接受通道的地址信息
	 * @return 接受收到的 UdpSocket 对象
	 * @throws IOException IO异常
	 */
	public UdpSocket udpAccept(UdpServerSocket socketContext, DatagramChannel datagramChannel, SocketAddress address) throws IOException {
		UdpSocket udpSocket = new UdpSocket(socketContext, datagramChannel, (InetSocketAddress) address);
		udpSocket.acceptStart();
		return udpSocket;
	}

	/**
	 * UDP 从通道读数据的方法
	 * @param socketContext SocketContext 对象
	 * @param datagramChannel 读取的 DatagramChannel 对象
	 * @return 读取数据的字节数, -1:读取失败
	 */
	public int udpReadFromChannel(SocketContext<DatagramChannel, UdpSession> socketContext, DatagramChannel datagramChannel) {
		try {
			int readSize = -1;

			//接受的连接isConnected 是 false
			//发起的连接isConnected 是 true
			if (datagramChannel.isConnected()) {
				readSize = datagramChannel.read(readTempBuffer);
			} else {
				socketContext = (UdpSocket) udpAccept((UdpServerSocket) socketContext, datagramChannel, datagramChannel.receive(readTempBuffer));
				UdpSession session = socketContext.getSession();
				readSize = readTempBuffer.position();
			}
			readSize = loadAndPrepare(socketContext.getSession(), readSize);

			return readSize;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}

	/**
	 * UDP 向通道写数据的方法
	 * @param socketContext UdpSocket 对象
	 * @param buffer 待写入的数据缓冲对象
	 * @return 写入数据的字节数, -1:写入失败
	 */
	public int udpWriteToChannel(UdpSocket socketContext, ByteBuffer buffer) {
		try {
			DatagramChannel datagramChannel = socketContext.socketChannel();
			UdpSession session = socketContext.getSession();

			int totalSendByte = 0;
			long start = System.currentTimeMillis();
			if (socketContext.isOpen() && buffer != null) {
				//循环发送直到全部内容发送完毕
				while (buffer.remaining() != 0) {
					int sendSize = 0;
					if (datagramChannel.isConnected()) {
						sendSize = datagramChannel.write(buffer);
					} else {
						sendSize = datagramChannel.send(buffer, session.getInetSocketAddress());
					}
					if (sendSize == 0) {
						TEnv.sleep(1);
						if (System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
							Logger.error("NioSelector udpWriteToChannel timeout, Socket will be close");
							socketContext.close();
							return -1;
						}
					} else {
						start = System.currentTimeMillis();
						totalSendByte += sendSize;
					}
				}
			}
			return totalSendByte;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}

	/**
	 * 数据读取
	 * @param session IoSession会话对象
	 * @param readSize 需要读取数据大小
	 * @return 实际读取的数据大小
	 * @throws IOException IO 异常
	 */
	public int loadAndPrepare(IoSession session, int readSize) throws IOException {
		ByteBufferChannel appByteBufferChannel = session.getReadByteBufferChannel();

		// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
		if (MessageLoader.isStreamEnd(readTempBuffer, readSize) || !session.isConnected()) {
			session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
			session.close();
			return -1;
		} else {

			readTempBuffer.flip();

			if (readSize > 0) {

				//如果缓冲队列已慢, 则等待可用, 超时时间为读超时
				try {
					TEnv.wait(session.socketContext().getReadTimeout(), () -> appByteBufferChannel.size() + readTempBuffer.limit() >= appByteBufferChannel.getMaxSize());
				} catch (TimeoutException e) {
					Logger.error("Session.byteByteBuffer is not enough:", e);
				}

				//如果在没有 SSL 支持 和 握手没有完成的情况下,直接写入
				if (!SSLParser.isHandShakeDone(session)) {
					session.getSSLParser().getSSlByteBufferChannel().writeEnd(readTempBuffer);
					session.getSSLParser().doHandShake();
				} else {
					//接收SSL数据, SSL握手完成后解包
					if (session.isSSLMode()) {
						session.getSSLParser().unWarpByteBufferChannel(readTempBuffer);
					} else {
						appByteBufferChannel.writeEnd(readTempBuffer);
					}

					//检查心跳
					if (session.getHeartBeat() != null) {
						//锁住appByteBufferChannel防止异步问题
						appByteBufferChannel.getByteBuffer();
						try {
							HeartBeat.interceptHeartBeat(session, appByteBufferChannel);
						} finally {
							appByteBufferChannel.compact();
						}
					}

					if (appByteBufferChannel.size() > 0) {
						// 触发 onReceive 事件
						EventTrigger.fireReceive(session);
					}
				}

				// 接收完成后重置buffer对象
				readTempBuffer.clear();
			}

			return readSize;
		}


	}

	static String BROKEN_PIPE = "Broken pipe";
	static String CONNECTION_RESET = "Connection reset by peer";

	/**
	 * 异常处理方法
	 * @param socketContext SocketContext 对象
	 * @param e Exception 异常对象
	 * @return 永远返回 -1
	 */
	public int dealException(SocketContext socketContext, Exception e) {
		if(BROKEN_PIPE.equals(e.getMessage()) || CONNECTION_RESET.equals(e.getMessage())){
			socketContext.close();
			return -1;
		}

		//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
		if(e.getStackTrace()[0].getClassName().contains("sun.tcp.ch")){
			return -1;
		}

		if(e instanceof Exception){
			//触发 onException 事件
			try {
				EventTrigger.fireException((IoSession) socketContext.getSession(), e);
			} catch (Exception ex) {
				e.printStackTrace();
			}
		}

		return -1;
	}

}
