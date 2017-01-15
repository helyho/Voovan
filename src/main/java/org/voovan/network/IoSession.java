package org.voovan.network;

import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 会话抽象类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class IoSession {
	
	private Map<Object, Object> attributes;
	private SSLParser sslParser;
	private ByteBufferChannel netDataBufferChannel;
	private ByteBufferChannel appDataBufferChannel;
	private boolean onReceive;

	/**
	 * 构造函数
	 */
	protected IoSession(){
		attributes = new ConcurrentHashMap<Object, Object>();
		netDataBufferChannel = new ByteBufferChannel();
		appDataBufferChannel = new ByteBufferChannel();
	}

	protected boolean isOnReceive() {
		return onReceive;
	}

	protected void setOnReceive(boolean onReceive) {
		this.onReceive = onReceive;
	}

	/**
	 * 获取接收的输出流
	 * @return 接收的输出流
	 */
	protected abstract ByteBufferChannel getByteBufferChannel();
	
	/**
	 * 获取 SSLParser
	 * @return SSLParser对象
	 */
	protected SSLParser getSSLParser() {
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
	public abstract String loaclAddress();
	
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
	public abstract SocketContext sockContext();
	
	/**
	 * 读取消息到缓冲区
	 * @param buffer    接收数据的缓冲区
	 * @return 接收数据大小
	 * @throws IOException IO 异常
	 */
	protected abstract int read(ByteBuffer buffer) throws IOException;
	
	
	/**
	 * 发送消息
	 * 		注意直接调用不会出发 onSent 事件
	 * @param buffer  发送缓冲区
	 * @return 读取的字节数
	 * @throws IOException IO 异常
	 */
	public abstract int send(ByteBuffer buffer) throws IOException;

	/**
	 * 同步读取消息
	 * @return 读取出的对象
	 * @throws ReadMessageException  读取消息异常
	 */
	public Object synchronouRead() throws ReadMessageException {
		Object readObject = null;
		while(true){
			readObject = getAttribute("SocketResponse");
			if(readObject!=null) {
				if(readObject instanceof Exception){
						throw new ReadMessageException("Method synchronouRead error! Error by " +
								((Exception) readObject).getClass().getSimpleName() + ".", (Exception) readObject);
				}
				removeAttribute("SocketResponse");
				break;
			}
			TEnv.sleep(1);
		}
		return readObject;
	}

	/**
	 * 同步发送消息
	 * @param obj  要发送的对象
	 * @throws SendMessageException  消息发送异常
	 */
	public void synchronouSend(Object obj) throws SendMessageException{
		//等待 ssl 握手完成
		while(sslParser!=null && !sslParser.handShakeDone){
			TEnv.sleep(1);
		}

		if (obj != null) {
			try {
				obj = EventProcess.filterEncoder(this,obj);
				EventProcess.sendMessage(this, obj);
			}catch (Exception e){
				throw new SendMessageException("Method synchronouSend error! Error by "+
						e.getClass().getSimpleName() + ".",e);
			}
		}
	}

	/**
	 * 读取SSL消息到缓冲区
	 * @param buffer    接收数据的缓冲区
	 * @return 接收数据大小
	 * @throws IOException  IO异常
	 */
	protected int readSSLData(ByteBuffer buffer) throws IOException{
		int readSize = 0;
		
		ByteBuffer netBuffer = sslParser.buildAppDataBuffer();
		ByteBuffer appBuffer = sslParser.buildAppDataBuffer();
		
		if(isConnect() && buffer!=null){
			SSLEngineResult engineResult = null;
			do{
				netBuffer.clear();
				appBuffer.clear();
				if(read(netBuffer)!=0){
					netDataBufferChannel.writeEnd(netBuffer);
					ByteBuffer byteBuffer = netDataBufferChannel.getBuffer();
					engineResult = sslParser.unwarpData(byteBuffer, appBuffer);
					netDataBufferChannel.writeHead(byteBuffer);
					appBuffer.flip();
					appDataBufferChannel.writeEnd(appBuffer);
				}
			}while(engineResult!=null && engineResult.getStatus() != Status.OK);
			readSize = appDataBufferChannel.readHead(buffer);
		}
		return readSize;
	}
	
	
	/**
	 * 发送SSL消息
	 * 		注意直接调用不会出发 onSent 事件
	 * 	@param buffer byte缓冲区
	 */
	public void sendSSLData(ByteBuffer buffer){
		if(isConnect() && buffer!=null){
			try {
				sslParser.warpData(buffer);
			} catch (IOException e) {
				Logger.error("Send SSL data failed.",e);
			}
		}
	}
	
	
	/**
	 * 获取消息处理类
	 * @return 消息处理类
	 */
	protected abstract MessageLoader getMessageLoader();
	
	/**
	 * 获取消息分割处理类
	 * @return 消息分割处理类
	 */
	protected abstract MessageSplitter getMessagePartition();
	
	/**
	 * 会话是否打开
	 * @return	true: 打开,false: 关闭
	 */
	public abstract boolean isConnect();
	
	/**
	 * 关闭会话
	 * @return 是否关闭
	 */
	public abstract boolean close();
	
	@Override
	public abstract String toString();
}
