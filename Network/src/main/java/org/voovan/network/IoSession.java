package org.voovan.network;

import org.voovan.Global;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.concurrent.*;


/**
 * 会话抽象类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class IoSession<T extends SocketContext> {
	private Map<Object, Object> attributes;
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

	/**
	 * 会话状态管理
	 */
	public class State {
		private boolean init = true;
		private boolean connect = false;
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
		attributes = new ConcurrentHashMap<Object, Object>();
		this.socketContext = socketContext;
		this.state = new State();
		readByteBufferChannel = new ByteBufferChannel(socketContext.getReadBufferSize());
		sendByteBufferChannel = new ByteBufferChannel(socketContext.getSendBufferSize());
		messageLoader = new MessageLoader(this);
		checkIdle();
	}


	protected EventRunner getEventRunner() {
		return socketContext().getEventRunner();
	}

	public SelectionKey getSelectionKey() {
		return selectionKey;
	}

	public void setSelectionKey(SelectionKey selectionKey) {
		this.selectionKey = selectionKey;
	}

	/**
	 * 获取心跳对象
	 * @return 心跳对象
	 */
	public HeartBeat getHeartBeat() {
		return heartBeat;
	}

	protected void setHeartBeat(HeartBeat heartBeat) {
		this.heartBeat = heartBeat;
	}

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
					@Override
					public void run() {
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
							this.cancel();
							return;
						}

						//检查空间时间
						if(socketContext.getIdleInterval() < 1){
							return;
						}

						//触发空闲事件
						long timeDiff = System.currentTimeMillis() - lastIdleTime;
						if (timeDiff >= socketContext.getIdleInterval() * 1000) {
							EventTrigger.fireIdle(session);
							lastIdleTime = System.currentTimeMillis();
						}

					}
				};

				checkIdleTask.run();

				Global.getHashWheelTimer().addTask(checkIdleTask, 1);
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
	 * 获取接收的输出流
	 *
	 * @return 接收的输出流
	 */
	public ByteBufferChannel getReadByteBufferChannel() {
		return readByteBufferChannel;
	}

	/**
	 * 获取发送收的输出流
	 *
	 * @return 发送的输出流
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
		if(this.sslParser==null){
			this.sslParser = sslParser;
		}
	}

	/**
	 * 获取全部会话参数
	 * @return 会话参数Map
	 */
	public Map<Object,Object> getAttributes(){
		return this.attributes;
	}

	/**
	 * 获取会话参数
	 * @param key 参数名
	 * @return    参数对象
	 */
	public Object getAttribute(Object key) {
		return attributes.get(key);
	}

	/**
	 * 设置会话参数
	 * @param key     参数名
	 * @param value   参数对象
	 */
	public void setAttribute(Object key, Object value) {
		this.attributes.put(key, value);
	}

	/**
	 * 移除会话参数
	 * @param key     参数名
	 */
	public void removeAttribute(Object key) {
		this.attributes.remove(key);
	}

	/**
	 * 检查会话参数是否存在
	 * @param key     参数名
	 * @return 是否包含
	 */
	public boolean containAttribute(Object key) {
		return this.attributes.containsKey(key);
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
		return socketContext().getSelector().readFromChannel();
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
		int waitedTime = 0;

		if(socketContext.handler() instanceof SynchronousHandler) {
			synchronousHandler = (SynchronousHandler) socketContext.handler();
		}else{
			throw new ReadMessageException("Use the syncRead method must set an object of SynchronousHandler into the socket handler ");
		}

		try {
			//如果响应对象不存在则继续循环等待直到结果出现
			SynchronousHandler finalSynchronousHandler = synchronousHandler;
			TEnv.wait(socketContext.getReadTimeout(), ()->!finalSynchronousHandler.hasNextResponse() && isConnected());

			if(isConnected()) {
				readObject = ((SynchronousHandler) socketContext.handler()).getResponse();
			}

			if(readObject == null && !isConnected()){
				throw new ReadMessageException("Method syncRead error! Socket is disconnected");
			}

			if(readObject instanceof Throwable){
				Exception exception = (Exception) readObject;
				if (exception != null) {
					removeAttribute("SocketException");
					throw new ReadMessageException("Method syncRead error! Error by " +
							exception.getClass().getSimpleName() + ". " + exception.getMessage(), exception);
				}
			} else {
				return readObject;
			}
		} catch (TimeoutException e) {
			throw new ReadMessageException("syncRead readFromChannel timeout or socket is disconnect");
		}
		return readObject;
	}


	/**
	 * 发送消息到 JVM 的 SocketChannel
	 * 		注意直接调用不会出发 onSent 事件
	 * @param buffer  发送缓冲区
	 * @return 读取的字节数
	 * @throws IOException IO 异常
	 */
	protected int send0(ByteBuffer buffer) throws IOException {
		return socketContext().getSelector().writeToChannel(buffer);
	}

	/**
	 * 发送消息到发送缓冲区
	 * @param buffer 发送到缓冲区的 ByteBuffer 对象
	 * @return 添加至缓冲区的字节数
	 */
	public int sendByBuffer(ByteBuffer buffer) {
		try {
			return sendByteBufferChannel.writeEnd(buffer);
		} catch (Exception e) {
			if (socketContext.isConnected()) {
				Logger.error("IoSession.sendByBuffer buffer failed", e);
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
			TEnv.wait(socketContext.getReadTimeout(), ()->sslParser!=null && !sslParser.handShakeDone);
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
				//warpData 内置调用 session.send0 将数据送至发送缓冲区
				sslParser.warpData(buffer);
				return buffer.limit();
			} else {
				//如果大于缓冲区,则现发送一次
				if(buffer.limit() + sendByteBufferChannel.size() > sendByteBufferChannel.getMaxSize()){
					flush();
				}
				return sendByBuffer(buffer);
			}
		} catch (IOException e) {
			Logger.error("IoSession.writeToChannel data failed" ,e);
		}

		return -1;
	}

	/**
	 * 推送缓冲区的数据到 socketChannel
	 */
	public void flush() {
		if(sendByteBufferChannel.size()>0) {
			try {
				send0(sendByteBufferChannel.getByteBuffer());
				//触发发送事件
				EventTrigger.fireFlush(this);
			} catch (IOException e) {
				if(isConnected()) {
					if (socketContext.isConnected()) {
						Logger.error("IoSession.flush buffer failed", e);
					}
				}
			} finally {
				sendByteBufferChannel.compact();
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
	protected abstract MessageSplitter getMessagePartition();

	/**
	 * 设置是否使用分割器读取
	 * @param useSpliter true 使用分割器读取,false 不使用分割器读取,且不会出发 onRecive 事件
	 */
	public void enabledMessageSpliter(boolean useSpliter) {
		messageLoader.enable(useSpliter);
	}

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



	@Override
	public abstract String toString();
}
