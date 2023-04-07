package org.voovan.network;

import org.voovan.Global;
import org.voovan.network.tcp.TcpServerSocket;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.network.udp.UdpServerSocket;
import org.voovan.network.udp.UdpSession;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.TEnv;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.collection.ArraySet;
import org.voovan.tools.event.EventRunner;
import org.voovan.tools.event.EventTask;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 选择器
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SocketSelector implements Closeable {
	private  EventRunner eventRunner;

	protected Selector selector;
	protected boolean isCheckTimeout;

	protected ArraySet<SelectionKey> selectedKeys = new ArraySet<SelectionKey>(65536);
	protected AtomicBoolean selecting = new AtomicBoolean(false);

	private Runnable selectEvent;

	/**
	 * 构造方法
	 * @param eventRunner 事件执行器
	 * @param isCheckTimeout 是否检查超时
	 * @throws IOException IO 异常
	 */
	public SocketSelector(EventRunner eventRunner, boolean isCheckTimeout) throws IOException {
		this.selector = SelectorProvider.provider().openSelector();
		this.eventRunner = eventRunner;
		this.isCheckTimeout = isCheckTimeout;

		NioUtil.transformSelector(selector, selectedKeys);

		selectEvent = ()->{
			try{
				select();
				eventRunner.addEvent(4, selectEvent);
			} catch (Exception e) {
				Logger.error("addChoseEvent error:", e);
			}
		};

		eventRunner.addEvent(4, selectEvent);

		//调试日志信息
		if(Global.IS_DEBUG_MODE) {
			Global.schedual(new HashWheelTask() {
				@Override
				public void run() {
					System.out.print(eventRunner.getThread().getName() + " " + selector.keys().size() + " = " + eventRunner.getEventQueue().size());

					int ioTaskCount = 0;
					int eventTaskCount = 0;
					int registerTaskCount = 0;
					for (EventTask eventTask : eventRunner.getEventQueue()) {
						if (eventTask.getPriority() == 4)
							ioTaskCount++;
						if (eventTask.getPriority() == 5)
							eventTaskCount++;
						if (eventTask.getPriority() == 6)
							registerTaskCount++;
					}

					System.out.println(" (IO=" + ioTaskCount + ", Event=" + eventTaskCount + " ,register=" + registerTaskCount + ")");
				}
			}, 1);
		}
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
		if(ops==0) {
			//udp
			IoSession session = socketContext.getSession();
			session.setSocketSelector(this);

			socketContext.setRegister(true);

			if (socketContext.connectModel != ConnectModel.LISTENER) {
				socketContext.unhold();
			}
		} else {
			//tcp
			addEvent(6, () -> {
				try {
					SelectionKey selectionKey = socketContext.socketChannel().register(selector, ops, socketContext);

					IoSession session = socketContext.getSession();

					if (socketContext.connectModel != ConnectModel.LISTENER) {
						session.setSelectionKey(selectionKey);
						session.setSocketSelector(this);
					}

					socketContext.setRegister(true);
				} catch (ClosedChannelException e) {
					Logger.error("Register " + socketContext + " to selector error", e);
				} finally {
					if (socketContext.connectModel != ConnectModel.LISTENER) {
						socketContext.unhold();
					}
				}
			});

     		//正在 select 则唤醒
			if (selecting.get()) {
				selector.wakeup();
			}
		}

		return true;
	}

	/**
	 * 在选择器中取消一个 SocketContext 的注册
	 * @param selectionKey SocketContext 对象
	 */
	public void unRegister(SelectionKey selectionKey) {

		//===================================== 处理 selectionKey =====================================
		try {
			selectionKey.channel().close();
		} catch (IOException e) {
			Logger.error(e);
		}

		if (selectionKey.isValid()) {
			selectionKey.interestOps(0);
		}

		//SocketChannel.close() 中会 cancel()
		//selectionKey.cancel();

		//正在 select 则唤醒, 需要在 cancel 后立刻处理 selectNow
		if (selecting.get()) {
			selector.wakeup();
		}

		//===================================== 处理 SocketContext =====================================
		SocketContext socketContext = (SocketContext) selectionKey.attachment();

		if(socketContext!=null && socketContext.isRegister() && selectionKey.channel().isRegistered()) {
			socketContext.setRegister(false);
			selectionKey.attach(null);

			EventTrigger.fireDisconnect(socketContext.getSession());
			socketContext.setFileDescriptor(null);
		}
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
	 *  @param priority 指定的事件优先级, 越大优先级越高, 1-3 预留事件等级, 4:IO 事件, 5:EventProcess 事件, 6: Socket 注册事件, 7-10 预留事件等级
	 * @param runnable 在事件选择前执行的方法
	 */
	public void addEvent(int priority, Runnable runnable){
		if(runnable==null) {
			throw new NullPointerException("add Event's second paramater must be not null");
		}

		if(selector.isOpen()) {
			eventRunner.addEvent(priority, runnable);
		}
	}

	int JvmEpollBugFlag = 0;

	/**
	 * 事件选择业务
	 * @return true: 有相关的 NIO 事件被处理, false: 无相关 NIO 事件被处理
	 */
	public boolean select() {
		boolean ret = false;

		// 事件循环
		try {
			if (selector != null && selector.isOpen()) {
				//执行选择操作, 如果还有可选择的 socket 注册的 key
				processSelect();

				//如果有待处理的操作则下次调用 selectNow, 如果没有待处理的操作则调用带有阻赛的 select
				if (!selectedKeys.isEmpty()) {
					ret = processSelectionKeys();
				}
			}
		} catch (IOException e){
			Logger.error("NioSelector error: ", e);
		}

		return ret;
	}

	/**
	 * 执行通道操作选择
	 * @throws IOException IO 异常
	 */
	private void processSelect() throws IOException {
		try {
				//检查超时
				checkReadTimeout();
				selecting.compareAndSet(false, true);
				NioUtil.select(selector, SocketContext.SELECT_INTERVAL);
				selecting.compareAndSet(true, false);
		} catch (Throwable e) {
			Logger.error(e);
		}
	}

	/**
	 * 读超时检查
	 */
	public void checkReadTimeout(){
		if(isCheckTimeout) {
			for (SelectionKey selectionKey : selector.keys()) {
				SocketContext socketContext = (SocketContext) selectionKey.attachment();
				if (socketContext!=null && socketContext.connectModel != ConnectModel.LISTENER) {

					//缓冲区是否有数据
					boolean bufferDataEmpty = socketContext.getSession().getReadByteBufferChannel().isEmpty();

					if(socketContext.isTimeOut() && bufferDataEmpty) {
						socketContext.close();
						EventTrigger.fireException(socketContext.getSession(), new TimeoutException("Socket Read timeout"));
					} else if(!bufferDataEmpty) {
						socketContext.updateLastTime();
					}
				}
			}
		}
	}

	/**
	 * 处理选择到的 Key
	 * @return true: 有相关的 NIO 事件被处理, false: 无相关 NIO 事件被处理
	 * @throws IOException IO 异常
	 */

	private boolean processSelectionKeys() throws IOException {
		boolean ret = false;
		for (int i = 0; i< selectedKeys.size(); i++) {
			SelectionKey selectedKey = selectedKeys.getAndRemove(i);

			if (selectedKey!=null && selectedKey.isValid()) {
				// 获取 socket 通道
				SelectableChannel channel = selectedKey.channel();
				SocketContext socketContext = (SocketContext) selectedKey.attachment();

				if (channel.isOpen() && selectedKey.isValid()) {
					//事件分发,包含时间 onRead onAccept
					{
						// Server接受连接
						if ((selectedKey.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
							SocketChannel socketChannel = ((ServerSocketChannel) channel).accept();
							tcpAccept((TcpServerSocket) socketContext, socketChannel);
						}

						// 有数据读取
						if ((selectedKey.readyOps() & SelectionKey.OP_READ) != 0) {
							readFromChannel(socketContext, channel);
						}
					}
				}
				ret = true;
			}
		}

		selectedKeys.reset();

		return ret;
	}

	/**
	 * 选择器关闭方法
	 */
	public void close() {
		try {
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
		try {
			int readSize = -1;

			if (selectableChannel instanceof SocketChannel) {
				readSize = tcpReadFromChannel((TcpSocket) socketContext, (SocketChannel) selectableChannel);
			} else if (selectableChannel instanceof DatagramChannel) {
				DatagramChannel datagramChannel = (DatagramChannel) selectableChannel;

				//udp accept new connection
				if (socketContext.getConnectModel() == ConnectModel.LISTENER && !datagramChannel.isConnected()) {
					socketContext = (UdpSocket) udpAccept((UdpServerSocket) socketContext, datagramChannel);
				}

				readSize = udpReadFromChannel((SocketContext<DatagramChannel, UdpSession>) socketContext, (DatagramChannel) selectableChannel);
			}

			IoSession session = socketContext.getSession();

			// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
			if (MessageLoader.isStreamEnd(readSize) || !session.isOpen()) {
				session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
				session.close();
				return -1;
			} else {

				//初始化状态不出发 loadAndPrepare
				if (!session.getState().isInit()) {
					readSize = loadAndPrepare(session, readSize);
				}
			}
			return readSize;
		} catch(Exception e){
			return dealException(socketContext, e);
		}
	}

	/**
	 * 通用封装的向通道写数据的方法
	 * @param socketContext SocketContext 对象
	 * @param buffer 待写入的数据缓冲对象
	 * @return 写入数据的字节数, -1:写入失败
	 */
	public int writeToChannel(SocketContext socketContext, ByteBuffer buffer){
		try {
			if (socketContext.getConnectType() == ConnectType.TCP) {
				return tcpWriteToChannel((TcpSocket) socketContext, buffer);
			} else if (socketContext.getConnectType() == ConnectType.UDP) {
				return udpWriteToChannel((UdpSocket) socketContext, buffer);
			} else {
				return -1;
			}
		} catch(Exception e) {
			return dealException(socketContext, e);
		} finally {
			socketContext.getSession().getSendByteBufferChannel().clear();
		}
	}

	/**
	 * Tcp 服务接受一个新的连接
	 * @param socketContext SocketContext 对象
	 * @param socketChannel Socketchannel 对象
	 */
	public void tcpAccept(TcpServerSocket socketContext, SocketChannel socketChannel) {
		TcpSocket socket = new TcpSocket(socketContext, socketChannel);
		EventTrigger.fireAccept(socket.getSession());
	}

	/**
	 * TCP 从通道读数据的方法
	 * @param socketContext TcpSocket 对象
	 * @param socketChannel 读取的 Socketchannel 对象
	 * @return 读取数据的字节数, -1:读取失败
	 * @throws IOException IO 异常
	 */
	public int tcpReadFromChannel(TcpSocket socketContext, SocketChannel socketChannel) throws IOException {
		IoSession session = socketContext.getSession();

		ByteBufferChannel byteBufferChannel = IoPlugin.getReadBufferChannelChain(socketContext);

		int readSize = -1;
		boolean isBufferFull = false;
		for(;;) {
			if (!byteBufferChannel.isReleased()) {
				ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

				try {
					//如果有历史数据则从历史数据尾部开始写入
					byteBuffer.position(byteBuffer.limit());
					byteBuffer.limit(byteBuffer.capacity());
					readSize = NioUtil.read(socketContext, byteBuffer);

					isBufferFull = !byteBuffer.hasRemaining();

					byteBuffer.flip();

					//自动扩容
					if(isBufferFull) {
						if(byteBufferChannel.available() == 0) {
							byteBufferChannel.reallocate(byteBufferChannel.capacity() + 256 * 1024);
						}
					} else {
						break;
					}
				} catch (Throwable e) {
					throw new IOException("SocketSelector.tcpReadFromChannel error: " + e.getMessage(), e);
				} finally {
					byteBufferChannel.compact();
				}
			}
		}

		return readSize;
	}

	/**
	 * TCP 向通道写数据的方法
	 * @param socketContext TcpSocket 对象
	 * @param buffer 待写入的数据缓冲对象
	 * @return 写入数据的字节数, -1:写入失败
	 * @throws IOException IO 异常
	 */
	public int tcpWriteToChannel(TcpSocket socketContext, ByteBuffer buffer) throws IOException {
		int totalSendByte = 0;
		int sendSize = - 1;

		long start = System.currentTimeMillis();
		if (buffer != null) {
			//循环发送直到全部内容发送完毕
			try {
				while (buffer.remaining() != 0) {
					sendSize = NioUtil.write(socketContext,buffer);

					if (sendSize == 0) {
						if (socketContext.getSendTimeout() >0 && System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
							Logger.error("SocketSelector tcpWriteToChannel timeout", new TimeoutException());
							socketContext.close();
							return -1;
						}
					} else if (sendSize < 0) {
						socketContext.close();
						return -1;
					} else {
						start = System.currentTimeMillis();
						totalSendByte += sendSize;
					}
				}
			} catch (NotYetConnectedException | ClosedChannelException e) {
				socketContext.close();
				return -1;
			} catch (Throwable e) {
				throw new IOException("SocketSelector.tcpWriteToChannel error: " + e.getMessage(), e);
			}
		}
		return totalSendByte;
	}

	/**
	 * UDP 服务接受一个新的连接
	 * @param socketContext UdpServerSocket 对象
	 * @param datagramChannel DatagramChannel 对象
	 * @return 接受收到的 UdpSocket 对象
	 * @throws IOException IO异常
	 */
	public UdpSocket udpAccept(UdpServerSocket socketContext, DatagramChannel datagramChannel) throws IOException {
		UdpSocket udpSocket = new UdpSocket(socketContext, datagramChannel, null);
		udpSocket.acceptStart();
		return udpSocket;
	}

	/**
	 * UDP 从通道读数据的方法
	 * @param socketContext SocketContext 对象
	 * @param datagramChannel 读取的 DatagramChannel 对象
	 * @return 读取数据的字节数, -1:读取失败
	 * @throws IOException IO 异
	 */
	public int udpReadFromChannel(SocketContext<DatagramChannel, UdpSession> socketContext, DatagramChannel datagramChannel) throws IOException {
		UdpSession session = socketContext.getSession();

		ByteBufferChannel byteBufferChannel = IoPlugin.getReadBufferChannelChain(socketContext);

		int readSize = -1;
		boolean isBufferFull = false;
		for(;;) {
			if (!byteBufferChannel.isReleased()) {
				ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

				try {
					//如果有历史数据则从历史数据尾部开始写入
					byteBuffer.position(byteBuffer.limit());
					byteBuffer.limit(byteBuffer.capacity());

					if (!datagramChannel.isConnected()) {
						SocketAddress socketAddress = datagramChannel.receive(byteBuffer);
						session.setInetSocketAddress((InetSocketAddress) socketAddress);
						readSize = byteBuffer.position();
					} else {
						readSize = datagramChannel.read(byteBuffer);
					}

					isBufferFull = !byteBuffer.hasRemaining();

					byteBuffer.flip();

					//自动扩容
					if(isBufferFull) {
						if(byteBufferChannel.available() == 0) {
							byteBufferChannel.reallocate(byteBufferChannel.capacity() + 256 * 1024);
						}
					} else {
						break;
					}
				} catch (Throwable e) {
					throw new IOException("SocketSelector.tcpReadFromChannel error: " + e.getMessage(), e);
				} finally {
					byteBufferChannel.compact();
				}
			}
		}

		return readSize;
	}

	/**
	 * UDP 向通道写数据的方法
	 * @param socketContext UdpSocket 对象
	 * @param buffer 待写入的数据缓冲对象
	 * @return 写入数据的字节数, -1:写入失败
	 * @throws IOException IO 异
	 */
	public int udpWriteToChannel(UdpSocket socketContext, ByteBuffer buffer) throws IOException {
		DatagramChannel datagramChannel = socketContext.socketChannel();
		UdpSession session = socketContext.getSession();

		int totalSendByte = 0;
		long start = System.currentTimeMillis();
		if (buffer != null) {
			try {
				//循环发送直到全部内容发送完毕
				while (buffer.remaining() != 0) {
					int sendSize = 0;
					if (datagramChannel.isConnected()) {
						sendSize = datagramChannel.write(buffer);
					} else {
						sendSize = datagramChannel.send(buffer, session.getInetSocketAddress());
					}

					if (sendSize == 0) {
						if (socketContext.getSendTimeout() >0 && System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
							Logger.error("SocketSelector udpWriteToChannel timeout, Socket will be close");
							socketContext.close();
							return -1;
						}
					} else if (sendSize < 0) {
						socketContext.close();
						return -1;
					} else {
						start = System.currentTimeMillis();
						totalSendByte += sendSize;
					}
				}
			} catch (NotYetConnectedException e) {
				socketContext.close();
				return -1;
			}
		}
		return totalSendByte;
	}

	/**
	 * 数据读取
	 * @param session IoSession会话对象
	 * @param readSize 需要读取数据大小
	 * @return 实际读取的数据大小
	 * @throws IOException IO 异常
	 */
	public int loadAndPrepare(IoSession session, int readSize) throws IOException {
		session.socketContext().updateLastTime();

		if (readSize > 0) {
			IoPlugin.unwrapChain(session.socketContext());

			if (!session.getState().isReceive() && session.getReadByteBufferChannel().size() > 0) {
				// 触发 onReceive 事件
				EventTrigger.fireReceive(session);
			}
		}

		return readSize;
	}

	static String BROKEN_PIPE = "Broken pipe";
	static String CONNECTION_RESET = "Connection reset";

	/**
	 * 异常处理方法
	 * @param socketContext SocketContext 对象
	 * @param e Exception 异常对象
	 * @return 永远返回 -1
	 */
	public int dealException(SocketContext socketContext, Exception e) {
		if(e.getMessage()!=null && (e.getMessage().contains(CONNECTION_RESET) || e.getMessage().endsWith(BROKEN_PIPE))){
			socketContext.close();
			return -1;
		}

		//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
		if(TEnv.OS_NAME.startsWith("WINDOWS") && e.getStackTrace()[0].getClassName().contains("sun.tcp.ch")){
			return -1;
		}

		if(e instanceof Exception){
			//触发 onException 事件
			try {
				EventTrigger.fireException((IoSession) socketContext.getSession(), e);
			} catch (Exception ex) {
				Logger.error(e);
			}
		}

		return -1;
	}

}
