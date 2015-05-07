package org.voovan.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;


/**
 * Socket消息处理类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MessageLoader {
	private IoSession session;
	private int readTimeout;
	

	/**
	 * 构造函数
	 * @param session
	 * @param readTimeOut 超时时间
	 */
	public MessageLoader(IoSession session,int readTimeout) {
		this.session = session;
		this.readTimeout = readTimeout;
	}

	/**
	 * 构造函数
	 * @param session
	 * @param readTimeOut 超时时间
	 */
	public MessageLoader(IoSession session) {
		this.session = session;
		this.readTimeout = 100;
	}
	
	public void setReadTimeOut(int readTimeout){
		this.readTimeout = readTimeout;
	}

	/**
	 * 判断连接是否意外断开
	 * @param length
	 * @param buffer
	 * @return
	 */
	public static boolean isRemoteClosed(Integer length,  ByteBuffer buffer){
		if(length==-1){
			//触发 disconnect 事件
			Logger.simple("Disconnect by Remote");
			return true;
		}
		//如果 buffer 被冲满,且起始、中位、结束的字节都是结束符(Ascii=4)则连接意外结束
	   if(length>2
				&& buffer.get(0)==4 //起始判断
				&& buffer.get(length/2)==4 //中位判断 
				&& buffer.get(length-1)==4){ //结束判断 
		   Logger.simple("Disconnect by Remote");
			return true;
		}
		
	   return false;
	}
	
	/**
	 * 读取 socket 中的数据
	 * 	逐字节读取数据,并用消息截断器判断消息包是否完整,消息粘包有两种截断方式:
	 * 	1.消息截断器生效
	 * 	2.消息读取时间超时,例如设置5m,则连续5秒内没有读取到有用的消息则返回报文.
	 * @param socketChannel
	 * @return
	 * @throws IOException
	 */
	public ByteBuffer read() throws IOException {
		
		if(session==null){
			return null;
		}
		
		MessageSplitter messageSplitter = session.sockContext().messageSplitter();
		
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		
		//当前已超时时间值
		int elapsedtime = 0;
		int wroteCount = 0;
		
		//缓冲区字段
		ByteBuffer buffer = ByteBuffer.allocate(1);
		
		if (session != null) {
			while (true) {
				buffer.clear();
				int readsize = 0;
				
				//读出数据
				if(session.getSSLParser()!=null && session.getSSLParser().handShakeDone){
					readsize = session.readSSLData(buffer);
				}
				else{
					readsize = session.read(buffer);
				}
				
				//将读出的数据写入缓冲区
				if(readsize!=0) {
					//如果读到数据,则清零超时事件
					wroteCount+=readsize;
					byteOutputStream.write(buffer.array(), 0, readsize);
				}
				
				//判断连接是否关闭
				if (isRemoteClosed(readsize,buffer)) {
					if(byteOutputStream.size()!=0){
						return buffer;
					}
					return null;
				}
				//使用消息划分器进行消息划分
				else if(messageSplitter!=null && messageSplitter.canSplite(session,byteOutputStream.toByteArray(), elapsedtime)){
					Logger.simple(byteOutputStream.size());
					break;
				}
				//readsize为0时认为是超时
				else if(readsize==0){
					
					//超时判断
					if(readTimeout==elapsedtime){
						break;
					}
					
					//如果连接断开立刻关闭
					if(!session.isConnect()){
						break;
					}
					
					TEnv.sleep(1);
					//超时时间自增
					elapsedtime++;
				}
			}
		}
		
		if(byteOutputStream.size()!=0){
			buffer = ByteBuffer.wrap(byteOutputStream.toByteArray());
		}
		
		if(wroteCount==0){
			buffer = ByteBuffer.allocate(0);
		}
		
		return buffer;
	}

	
	/**
	 * 将 ByteBuffer 转换成 String
	 * @param buf   byteBuffer 对象
	 * @return
	 */
	public static String byteBufferToString(ByteBuffer byteBuffer) {
		int size = byteBuffer.limit();
		byte[] byteBuf = new byte[size];
		byteBuffer.get(byteBuf);
		return new String(byteBuf);
	}
}
