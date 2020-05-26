package org.voovan.network;

import org.voovan.Global;
import org.voovan.network.tcp.TcpServerSocket;
import org.voovan.network.udp.UdpServerSocket;
import org.voovan.network.udp.UdpSession;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.*;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.collection.ArraySet;
import org.voovan.tools.event.EventRunner;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

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
 * @author: helyho
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
	private boolean useSelectNow = false;

	/**
	 * 构造方法
	 * @param eventRunner 事件执行器
	 * @param isCheckTimeout 是否检查超时
	 * @throws IOException IO 异常
	 */
	public SocketSelector(EventRunner eventRunner, boolean isCheckTimeout) throws IOException {
		this.selector = SelectorProvider.provider().openSelector();
		this.eventRunner = eventRunner;
		this.isCheckTimeout = SocketContext.CHECK_TIMEOUT == null ? isCheckTimeout : false;

		try {
			TReflect.setFieldValue(selector, NioUtil.selectedKeysField, selectedKeys);
			TReflect.setFieldValue(selector, NioUtil.publicSelectedKeysField, selectedKeys);
		} catch (ReflectiveOperationException e) {
			Logger.error(e);
		}

		addIoEvent();

		//调试日志信息
		if(Global.IS_DEBUG_MODE) {
			Global.getHashWheelTimer().addTask(new HashWheelTask() {
				@Override
				public void run() {
					System.out.print(eventRunner.getThread().getName() + " " + selector.keys().size() + " = " + eventRunner.getEventQueue().size());

					int ioTaskCount = 0;
					int eventTaskCount = 0;
					int registerTaskCount = 0;
					for (EventRunner.EventTask eventTask : eventRunner.getEventQueue()) {
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
			IoSession session = socketContext.getSession();
			session.setSocketSelector(this);
		} else {
			addEvent(6, () -> {
				try {
					SelectionKey selectionKey = socketContext.socketChannel().register(selector, ops, socketContext);

					if (socketContext.connectModel != ConnectModel.LISTENER) {
						IoSession session = socketContext.getSession();

						session.setSelectionKey(selectionKey);
						session.setSocketSelector(this);

						if (!session.isSSLMode()) {
							EventTrigger.fireConnect(session);
						} else {
							//客户端模式主动发起 SSL 握手的第一个请求
							if (socketContext.connectModel == ConnectModel.CLIENT) {
								session.getSSLParser().doHandShake();
							}
						}
					}

					socketContext.setRegister(true);
				} catch (ClosedChannelException e) {
					Logger.error("Register " + socketContext + " to selector error", e);
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
		selectionKey.cancel();

		//正在 select 则唤醒, 需要在 cancel 后立刻处理 selectNow
		if (selecting.get()) {
			selector.wakeup();
		}

		//===================================== 处理 SocketContext =====================================
		SocketContext socketContext = (SocketContext) selectionKey.attachment();

		if(socketContext!=null && socketContext.isRegister() && selectionKey.channel().isRegistered()) {
			socketContext.setRegister(false);
			selectionKey.attach(null);

			socketContext.getSession().getReadByteBufferChannel().release();
			socketContext.getSession().getSendByteBufferChannel().release();
			if (socketContext.getSession().isSSLMode()) {
				socketContext.getSession().getSSLParser().release();
			}

			EventTrigger.fireDisconnect(socketContext.getSession());
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
	 */
	public void addIoEvent(){
		eventRunner.addEvent(4, () -> {
			select();
			addIoEvent();
		});
	}

	/**
	 * 向执行器中增加一个选择事件
	 * @param priority 指定的事件优先级, 越小优先级越高, 1-3 预留事件等级, 4:IO 事件, 5:EventProcess 事件, 6: Socket 注册/注销事件, 7-10 预留事件等级
	 * @param runnable 在事件选择前执行的方法
	 */
	public void addEvent(int priority, Runnable runnable){
		if(runnable==null) {
			throw new NullPointerException("add Event's second paramater must be not null");
		}

		if(selector.isOpen()) {
			eventRunner.addEvent(priority, () -> {
				try {
					runnable.run();
				} catch (Exception e) {
					Logger.error("addChoseEvent error:", e);
				}
			});
		}
	}

	int JvmEpollBugFlag = 0;

	/**
	 * 事件选择业务
	 */
	public void select() {
		// 事件循环
		try {
			if (selector != null && selector.isOpen()) {
				//执行选择操作, 如果还有可选择的 socket 注册的 key
				processSelect();

				//如果有待处理的操作则下次调用 selectNow, 如果没有待处理的操作则调用带有阻赛的 select
				if (!selectedKeys.isEmpty()) {
					processSelectionKeys();
					useSelectNow = true;
				} else {
					useSelectNow = false;
				}
			}
		} catch (IOException e){
			Logger.error("NioSelector error: ", e);
		}
	}

	/**
	 * 执行通道操作选择
	 * @throws IOException IO 异常
	 */
	private void processSelect() throws IOException {
		if(useSelectNow){
			selector.selectNow();
		} else {
			try {
				//检查超时
				checkReadTimeout();
				selecting.compareAndSet(false, true);
				selector.select(SocketContext.SELECT_INTERVAL);
				selecting.compareAndSet(true, false);
			} catch (IOException e) {
				Logger.error(e);
			}
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
					boolean bufferDataEmpty = socketContext.getSession().getReadByteBufferChannel().isEmpty() && socketContext.getSession().getSendByteBufferChannel().isEmpty();

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
	 * @throws IOException IO 异常
	 */

	private void processSelectionKeys() throws IOException {
		for (int i = 0; i< selectedKeys.size(); i++) {
			SelectionKey selectedKey = selectedKeys.getAndRemove(i);

			if (selectedKey.isValid()) {
				// 获取 socket 通道
				SelectableChannel channel = selectedKey.channel();
				SocketContext socketContext = (SocketContext) selectedKey.attachment();
				if (channel.isOpen() && selectedKey.isValid()) {
					// 事件分发,包含时间 onRead onAccept
					// Server接受连接
					if((selectedKey.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
						SocketChannel socketChannel = ((ServerSocketChannel) channel).accept();
						tcpAccept((TcpServerSocket) socketContext, socketChannel);
					}

					// 有数据读取
					if ((selectedKey.readyOps() & SelectionKey.OP_READ) != 0) {
						socketContext.updateLastTime();
						readFromChannel(socketContext, channel);
					}
				}
			}
//			else {
//				unRegister(selectionKey);
//			}
		}

		selectedKeys.reset();
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
			if (selectableChannel instanceof SocketChannel) {
				return tcpReadFromChannel((TcpSocket) socketContext, (SocketChannel) selectableChannel);
			} else if (selectableChannel instanceof DatagramChannel) {
				return udpReadFromChannel((SocketContext<DatagramChannel, UdpSession>) socketContext, (DatagramChannel) selectableChannel);
			} else {
				return -1;
			}
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
			socketContext.updateLastTime();

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

		ByteBufferChannel byteBufferChannel = session.isSSLMode()
								? session.getSSLParser().getSSlByteBufferChannel()
								: session.getReadByteBufferChannel();

		//自动扩容
		if(byteBufferChannel.available() == 0) {
			byteBufferChannel.reallocate(byteBufferChannel.capacity() + 4 * 1024);
		}

		int readSize = -1;

		if(!byteBufferChannel.isReleased()) {

			ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

			//如果有历史数据则从历史数据尾部开始写入
			byteBuffer.position(byteBuffer.limit());
			byteBuffer.limit(byteBuffer.capacity());

			readSize = socketChannel.read(byteBuffer);

			byteBuffer.flip();
			byteBufferChannel.compact();
		}

		readSize = loadAndPrepare(socketContext.getSession(), readSize);
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
		long start = System.currentTimeMillis();
		if (socketContext.isConnected() && buffer != null) {
			//循环发送直到全部内容发送完毕
			while (socketContext.isConnected() && buffer.remaining() != 0) {
				int sendSize = socketContext.socketChannel().write(buffer);
				if (sendSize == 0) {
					if (System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
						Logger.error("SocketSelector tcpWriteToChannel timeout", new TimeoutException());
						socketContext.close();
						return -1;
					}
				} if (sendSize < 0){
					socketContext.close();
					return -1;
				} else {
					start = System.currentTimeMillis();
					totalSendByte += sendSize;
				}
			}
		}
		return totalSendByte;
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
	 * @throws IOException IO 异
	 */
	public int udpReadFromChannel(SocketContext<DatagramChannel, UdpSession> socketContext, DatagramChannel datagramChannel) throws IOException {


		if (!datagramChannel.isConnected()) {
			socketContext = (UdpSocket) udpAccept((UdpServerSocket) socketContext, datagramChannel, null);
		}

		UdpSession session = socketContext.getSession();

		ByteBufferChannel byteBufferChannel = session.getReadByteBufferChannel();
		ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

		//自动扩容
		if(byteBufferChannel.available() == 0) {
			byteBufferChannel.reallocate(byteBufferChannel.capacity() + 4 * 1024);
		}

		int readSize = -1;

		if(!byteBufferChannel.isReleased()) {

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

			byteBuffer.flip();
			byteBufferChannel.compact();
		}

		readSize = loadAndPrepare(socketContext.getSession(), readSize);

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
						Logger.error("SocketSelector udpWriteToChannel timeout, Socket will be close");
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
	}

	/**
	 * 数据读取
	 * @param session IoSession会话对象
	 * @param readSize 需要读取数据大小
	 * @return 实际读取的数据大小
	 * @throws IOException IO 异常
	 */
	public int loadAndPrepare(IoSession session, int readSize) throws IOException {

		// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
		if (MessageLoader.isStreamEnd(readSize) || !session.isConnected()) {
			session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
			session.close();
			return -1;
		} else {
			ByteBufferChannel appByteBufferChannel = session.getReadByteBufferChannel();

			if (readSize > 0) {
				if(session.isSSLMode()) {
					//如果在没有 SSL 支持 和 握手没有完成的情况下,直接写入
					if (!SSLParser.isHandShakeDone(session)) {
						session.getSSLParser().doHandShake();
						return readSize;
					} else {
						session.getSSLParser().unWarpByteBufferChannel();
					}
				}

				if (session.isConnected() && !session.getState().isReceive() && appByteBufferChannel.size() > 0) {
					// 触发 onReceive 事件
					if(SocketContext.ASYNC_RECIVE) {
						EventTrigger.fireReceiveAsync(session);
					} else {
						EventTrigger.fireReceive(session);
					}
				}
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
				Logger.error(e);
			}
		}

		return -1;
	}

}
