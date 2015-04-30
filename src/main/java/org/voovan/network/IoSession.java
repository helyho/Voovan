package org.voovan.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Map;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.Status;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;


/**
 * 会话抽象类
 * 
 * @author helyho
 *
 */
public abstract class IoSession {
	
	private Map<Object, Object> attributes;
	private SSLParser sslParser;
	private ByteBufferChannel netDataBufferChannel;
	private ByteBufferChannel appDataBufferChannel;
	
	/**
	 * 构造函数
	 */
	public IoSession(){
		attributes = new Hashtable<Object, Object>();
		netDataBufferChannel = new ByteBufferChannel();
		appDataBufferChannel = new ByteBufferChannel();
	}
	
	/**
	 * 获取接收的输出流
	 * @return
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
	 * @return SSLParser对象
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
	 * 检查会话参数是否存在
	 * @param key     参数名
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
	 * @param buffer    接受数据的缓冲区
	 * @return
	 */
	protected abstract int read(ByteBuffer buffer) throws IOException;
	
	
	/**
	 * 发送消息
	 * 		不出发任何事件
	 * @param byteBuffer
	 * @throws IOException
	 */
	protected abstract void send(ByteBuffer buffer) throws IOException;
	
	/**
	 * 读取SSL消息到缓冲区
	 * @param buffer    接受数据的缓冲区
	 * @return
	 */
	protected int readSSLData(ByteBuffer buffer){
		int readSize = 0;
		
		ByteBuffer netBuffer = sslParser.buildAppDataBuffer();
		ByteBuffer appBuffer = sslParser.buildAppDataBuffer();
		try{
			if(isConnect() && buffer!=null){
				SSLEngineResult engineResult = null;
				do{
					netBuffer.clear();
					appBuffer.clear();
					if(read(netBuffer)!=0){
						netDataBufferChannel.write(netBuffer);
						engineResult = sslParser.unwarpData(netDataBufferChannel.getBuffer(), appBuffer);
						appBuffer.flip();
						appDataBufferChannel.write(appBuffer);
					}
				}while(engineResult==null?false:engineResult.getStatus() != Status.OK);
			}
			readSize = appDataBufferChannel.read(buffer);
		}
		catch(Exception e){
			Logger.error("Class IoSession Error: "+e.getMessage());
			e.printStackTrace();
		}
		return readSize;
	}
	
	
	/**
	 * 发送SSL消息
	 * 		不出发任何事件
	 * @param byteBuffer
	 * @throws IOException
	 */
	protected void sendSSLData(ByteBuffer buffer){
		if(isConnect() && buffer!=null){
			try {
				sslParser.warpData(buffer);
			} catch (IOException e) {
				Logger.error("Class IoSession Error: "+e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 获取消息处理类
	 * @return
	 */
	protected abstract MessageLoader getMessageLoader();
	
	/**
	 * 获取消息处理类
	 * @return
	 */
	protected abstract MessageParter getMessagePartition();
	
	/**
	 * 会话是否打开
	 * @return	true: 打开,false: 关闭
	 */
	public abstract boolean isConnect();
	
	/**
	 * 关闭会话
	 */
	public abstract boolean close();
	
	@Override
	public abstract String toString();
}
