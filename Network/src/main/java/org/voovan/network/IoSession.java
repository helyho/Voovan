package org.voovan.network;

import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.collection.Attributes;
import org.voovan.tools.event.EventRunner;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.TimeoutException;

/**
 * 会话抽象类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class IoSession<T extends SocketContext> extends Attributes {
	public static HashWheelTimer SOCKET_IDLE_WHEEL_TIME = null;

	public static HashWheelTimer getIdleWheelTimer() {
		if(SOCKET_IDLE_WHEEL_TIME == null) {
			synchronized (IoSession.class) {
				if(SOCKET_IDLE_WHEEL_TIME == null) {
					SOCKET_IDLE_WHEEL_TIME = new HashWheelTimer("SocketIdle", 60, 1000);
					SOCKET_IDLE_WHEEL_TIME.rotate();
				}
			}
		}

		return SOCKET_IDLE_WHEEL_TIME;
	}

	private boolean sslMode = false;
	private SSLParser sslParser;


	private MessageLoader messageLoader;
	protected ByteBufferChannel readByteBufferChannel;
	protected ByteBufferChannel sendByteBufferChannel;
	private T socketContext;
	private long lastIdleTime = -1;
	private HashWheelTask checkIdleTask;
	private HeartBeat heartBeat;
	private State state;
	private SelectionKey selectionKey;
	private SocketSelector socketSelector;

	private Object attachment;

	/**
	 * 会话状态管理
	 */
	public class State {
		private boolean init = true;
		private boolean connect = false;
		private boolean receive = false;
		private boolean send = false;
		private boolean flush = false;
		private boolean close = false;

		public boolean isInit() {
			return init;
		}

		public void setInit(boolean init) {
			this.init = init;
		}

		public boolean isConnect() {
			return connect;
		}

		public void setConnect(boolean connect) {
			this.connect = connect;
		}

		public boolean isReceive() {
			return receive;
		}

		public void setReceive(boolean receive) {
			this.receive = receive;
		}

		public boolean isSend() {
			return send;
		}

		public void setSend(boolean send) {
			this.send = send;
		}

		public boolean isFlush() {
			return flush;
		}

		public void setFlush(boolean flush) {
			this.flush = flush;
		}

		public boolean isClose() {
			return close;
		}

		public void setClose(boolean close) {
			this.close = close;
		}
	}

	/**
	 * 构造函数
	 * @param socketContext socketContext对象
	 */
	public IoSession(T socketContext){
		this.socketContext = socketContext;
		this.state = new State();
		readByteBufferChannel = new ByteBufferChannel(socketContext.getReadBufferSize());
		sendByteBufferChannel = new ByteBufferChannel(socketContext.getSendBufferSize());
		readByteBufferChannel.setThreadSafe(SocketContext.ASYNC_RECIVE);
		sendByteBufferChannel.setThreadSafe(SocketContext.ASYNC_SEND);
		messageLoader = new MessageLoader(this);
		checkIdle();
	}

	public Object getAttachment() {
		return attachment;
	}

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	/**
	 * 获取 Socket 选择器
	 * @return Socket 选择器
	 */
	public SocketSelector getSocketSelector() {
		return socketSelector;
	}

	/**
	 * 设置 Socket 选择器
	 * @param socketSelector Socket 选择器
	 */
	protected void setSocketSelector(SocketSelector socketSelector) {
		this.socketSelector = socketSelector;
	}

	/**
	 * 获取 Event 执行器
	 * @return Event 执行器
	 */
	protected EventRunner getEventRunner() {
		return socketSelector.getEventRunner();
	}

	/**
	 * 获取 SelectionKey
	 * @return SelectionKey 对象
	 */
	protected SelectionKey getSelectionKey() {
		return selectionKey;
	}

	/**
	 * 设置 SelectionKey
	 * @param selectionKey SelectionKey 对象
	 */
	void setSelectionKey(SelectionKey selectionKey) {
		this.selectionKey = selectionKey;
	}

	/**
	 * 获取心跳对象
	 * @return 心跳对象
	 */
	public HeartBeat getHeartBeat() {
		return heartBeat;
	}

	/**
	 * 设置心跳对象
	 * @return 心跳对象
	 */
	void setHeartBeat(HeartBeat heartBeat) {
		this.heartBeat = heartBeat;
	}

	/**
	 * 获取状态
	 * @return 当前状态
	 */
	public State getState() {
		return state;
	}

	/**
	 * 启动空闲事件触发
	 */
	public void checkIdle(){
		if(socketContext.getIdleInterval() > 0) {

			if(checkIdleTask == null){
				final IoSession session = this;

				checkIdleTask = new HashWheelTask() {
					public void run() {
						//触发空闲事件
						long timeDiff = System.currentTimeMillis() - lastIdleTime;
						if (timeDiff >= socketContext.getIdleInterval() * 1000) {
							boolean isConnect = false;

							//初始化状态
							if(session.state.isInit() ||
									session.state.isConnect()) {
								return;
							}

							//检测会话状态
							if(session.state.isClose()){
								session.cancelIdle();
								return;
							}

							//获取连接状态
							isConnect = session.isConnected();

							if(!isConnect){
								session.cancelIdle();
								session.close();
								this.cancel();
								return;
							}


							//检查空间时间
							if(socketContext.getIdleInterval() < 1){
								return;
							}

							EventTrigger.fireIdle(session);
							lastIdleTime = System.currentTimeMillis();
						}

					}
				};

				checkIdleTask.run();

				getIdleWheelTimer().addTask(checkIdleTask, 1, true);
			}
		}
	}

	/**
	 * 停止空闲事件触发
	 */
	public void cancelIdle(){
		if(checkIdleTask!=null) {
			checkIdleTask.cancel();
			checkIdleTask = null;

			if(heartBeat!=null){
				heartBeat = null;
			}
		}
	}


	/**
	 * 获取空闲事件时间
	 * @return 空闲事件时间
	 */
	public int getIdleInterval() {
		return socketContext.getIdleInterval();
	}

	/**
	 * 设置空闲事件时间
	 * @param idleInterval  空闲事件时间
	 */
	public void setIdleInterval(int idleInterval) {
		socketContext.setIdleInterval(idleInterval);
	}

	/**
	 * 获取读取缓冲区
	 *
	 * @return 读取缓冲区
	 */
	public ByteBufferChannel getReadByteBufferChannel() {
		return readByteBufferChannel;
	}

	/**
	 * 获取发送缓冲区
	 *
	 * @return 发送缓冲区
	 */
	public ByteBufferChannel getSendByteBufferChannel() {
		return sendByteBufferChannel;
	}

	/**
	 * 获取 SSLParser
	 * @return SSLParser对象
	 */
	public SSLParser getSSLParser() {
		return sslParser;
	}

	/**
	 * 获取 SSLParser
	 * @param sslParser SSL解析对象
	 */
	protected void setSSLParser(SSLParser sslParser) {
		if(this.sslParser == null && sslParser!=null){
			this.sslParser = sslParser;
			sslMode = true;
		}
	}

	public boolean isSSLMode() {
		return sslMode;
	}

	/**
	 * 获取本地 IP 地址
	 * @return	本地 IP 地址
	 */
	public abstract String localAddress();

	/**
	 * 获取本地端口
	 * @return 返回-1为没有取到本地端口
	 */
	public abstract int loaclPort();

	/**
	 * 获取对端 IP 地址
	 * @return  对端 ip 地址
	 */
	public abstract String remoteAddress();

	/**
	 * 获取对端端口
	 * @return 	返回-1为没有取到对端端口
	 */
	public abstract int remotePort();

	/**
	 * 获取 socket 连接上下文
	 * @return	socket 连接上下文, 连接断开时返回的是null
	 */
	public T socketContext() {
		return socketContext;
	};

	/**
	 * 读取消息到缓冲区
	 * @return 接收数据大小
	 * @throws IOException IO 异常
	 */
	protected int read0() throws IOException {
		return socketSelector.readFromChannel(socketContext, socketContext.socketChannel());
	}

	/**
	 * 直接从缓冲区读取数据
	 * @param byteBuffer 字节缓冲对象ByteBuffer,读取 前需要使用 enabledMessageSpliter(false) 停止分割器的工作,除非有特殊的需求.
	 * @return  读取的字节数
	 * @throws IOException IO异常
	 * */
	public int read(ByteBuffer byteBuffer) throws IOException {

		int readSize = -1;

		if (byteBuffer != null && !this.getReadByteBufferChannel().isReleased()) {
			readSize = this.getReadByteBufferChannel().readHead(byteBuffer);
		}

		if(!this.isConnected() && readSize <= 0){
			readSize = -1;
			close();
		}

		return readSize;
	}

		/**
         * 同步读取消息
         * 			消息会经过 filter 的 decoder 函数处理后再返回
         * @return 读取出的对象
         * @throws ReadMessageException  读取消息异常
         */
	public Object syncRead() throws ReadMessageException {

		Object readObject = null;
		SynchronousHandler synchronousHandler = null;

		if(socketContext.handler() instanceof SynchronousHandler) {
			synchronousHandler = (SynchronousHandler) socketContext.handler();
		}else{
			throw new ReadMessageException("Use the syncRead method must set an object of SynchronousHandler into the socket handler ");
		}

		try {
			if(isConnected()) {
				readObject = synchronousHandler.getResponse(socketContext.getReadTimeout());
			} else {
				throw new ReadMessageException("syncRead failed, Socket is disconnected");
			}

			if(readObject == null) {
				if(isConnected()) {
					readObject = synchronousHandler.getResponse(socketContext.getReadTimeout());
					if(readObject == null) {
						throw new ReadMessageException("syncRead failed, resposne is null");
					}
				} else {
					throw new ReadMessageException("syncRead failed, Socket is disconnected");
				}
			}

			if(readObject instanceof Throwable){
				Exception exception = (Exception) readObject;
				if (exception != null) {
					throw new ReadMessageException("syncRead failed, Error by " + exception.getMessage(), exception);
				}
			} else {
				return readObject;
			}
		} catch (ReadMessageException re) {
			throw re;
		} catch (TimeoutException te) {
			try {
				//区分连接状态
				if (isConnected()) {
					throw new ReadMessageException("syncRead failed, socket is timeout", te);
				} else {
					throw new ReadMessageException("syncRead failed by timeout, Socket already disconnect", te);
				}
			} finally {
				socketContext.close();
			}
		} catch (Exception e) {
			throw new ReadMessageException("syncRead failed", e);
		}
		return readObject;
	}

	/**
	 * 发送消息到 JVM 的 SocketChannel
	 * 		注意直接调用不会出发 onSent 事件
	 * @param buffer  发送缓冲区
	 * @return 读取的字节数
	 */
	protected int send0(ByteBuffer buffer) {
		if(socketSelector != null) {
		return socketSelector.writeToChannel(socketContext, buffer);
		} else {
			return -1;
		}
	}

	/**
	 * 发送消息到发送缓冲区
	 * @param buffer 发送到缓冲区的 ByteBuffer 对象
	 * @return 添加至缓冲区的字节数
	 */
	protected int sendToBuffer(ByteBuffer buffer) {
		try {
			//如果大于缓冲区,则现发送一次
			if(buffer.limit() + sendByteBufferChannel.size() > sendByteBufferChannel.getMaxSize()){
				flush();
			}

			socketContext.updateLastTime();
			return sendByteBufferChannel.writeEnd(buffer);
		} catch (Exception e) {
			if (socketContext.isConnected()) {
				Logger.error("IoSession.sendByBuffer buffer failed", e);
			} else {
				close();
			}
		}

		return -1;
	}

	/**
	 * 同步发送消息
	 * 			消息会经过 filter 的 encoder 函数处理后再发送
	 * @param obj  要发送的对象
	 * @throws SendMessageException  消息发送异常
	 */
	public void syncSend(Object obj) throws SendMessageException{
		//等待 ssl 握手完成
		try {
			if(sslParser!=null) {
				TEnv.waitThrow(socketContext.getReadTimeout(), ()->!sslParser.handShakeDone);
			}

			if (obj != null) {
				try {
					EventProcess.sendMessage(this, obj);
					flush();
				}catch (Exception e){
					throw new SendMessageException("Method syncSend error! Error by "+
							e.getClass().getSimpleName() + ".",e);
				}
			}
		} catch (TimeoutException e) {
			throw new SendMessageException("Method syncSend error! Error by "+
					e.getClass().getSimpleName() + ".",e);
		}
	}

	/**
	 * 直接向缓冲区发送消息
	 * 		注意直接调用不会触发 onSent 事件, 也不会经过任何过滤器
	 * 	@param buffer byte缓冲区
	 * 	@return 发送的数据大小
	 */
	public int send(ByteBuffer buffer){
		try {
			if(sslParser!=null && sslParser.isHandShakeDone()) {
				//warpData 内置调用 session.sendByBuffer 将数据送至发送缓冲区
				sslParser.warpData(buffer);
				return buffer.limit();
			} else {
				return sendToBuffer(buffer);
			}
		} catch (IOException e) {
			Logger.error("IoSession.writeToChannel data failed" ,e);
		}

//		finally {
//			//同步模式自动 flush
//			if(socketContext.handler instanceof SynchronousHandler) {
//				flush();
//			}
//		}

		return -1;
	}

	/**
	 * 推送缓冲区的数据到 socketChannel
	 */
	public void flush() {
		if(sendByteBufferChannel.size()>0) {
			state.setFlush(true);
			ByteBuffer byteBuffer = sendByteBufferChannel.getByteBuffer();
			try {
				int size = send0(byteBuffer);
				if(size >= 0) {
					//ssl 握手完成后才触发 flush 事件
				    if(!sslMode || sslParser.isHandShakeDone()) {
						//触发发送事件
						EventTrigger.fireFlush(this);
					}
				} else {
					this.close();
				}
			} finally {
				sendByteBufferChannel.compact();
				state.setFlush(false);
			}
		}
	}

	/**
	 * 获取消息处理类
	 * @return 消息处理类
	 */
	public MessageLoader getMessageLoader() {
		return messageLoader;
	}

	/**
	 * 获取消息分割处理类
	 * @return 消息分割处理类
	 */
	protected abstract MessageSplitter getMessageSplitter();

	/**
	 * 会话是否连接
	 * @return	true: 连接,false: 关闭
	 */
	public abstract boolean isConnected();

	/**
	 * 会话是否打开
	 * @return	true: 打开,false: 关闭
	 */
	public abstract boolean isOpen();

	/**
	 * 关闭会话
	 * @return 是否关闭
	 */
	public abstract boolean close();

	public void release() {
		if(socketContext.isRegister() && socketSelector!=null) {
			socketSelector.unRegister(selectionKey);
		}

		readByteBufferChannel.release();
		sendByteBufferChannel.release();
		if (isSSLMode()) {
			sslParser.release();
		}

		cancelIdle();
	}

	@Override
	public abstract String toString();
}
