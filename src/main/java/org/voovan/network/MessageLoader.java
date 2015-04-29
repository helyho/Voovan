package org.voovan.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;


/**
 * Socket消息处理类
 * @author helyho
 *
 */
public class MessageLoader {
	private IoSession session;
	private int readTimeout;
	private int bufferSize;
	

	/**
	 * 构造函数
	 * @param session
	 * @param readTimeOut 超时时间
	 */
	public MessageLoader(IoSession session,int readTimeout,int bufferSize) {
		this.session = session;
		this.readTimeout = readTimeout;
		this.bufferSize = bufferSize;
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
			return true;
		}
		//如果 buffer 被冲满,且起始、中位、结束的字节都是结束符(Ascii=4)则连接意外结束
	   if(length>2
				&& buffer.get(0)==4 //起始判断
				&& buffer.get(length/2)==4 //中位判断 
				&& buffer.get(length-1)==4){ //结束判断 
			return true;
		}
		
	   return false;
	}
	
	/**
	 * 读取 socket 中的数据
	 * @param socketChannel
	 * @return
	 * @throws IOException
	 */
	public ByteBuffer read() throws IOException {
		//标记正在读取数据,标志改为 true ,主要是为了防止多线程读取数据导致 onRecive 事件触发的都是数据块
		
		MessageParter messageParter = session.sockContext().messageParter();
		
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		
		//当前已超时时间值
		int elapsedtime = 0;
		int wroteCount = 0;
		
		//缓冲区字段
		ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
		
		if (session != null) {
			while (true) {
				buffer.clear();
				int readsize = 0;
				
				if(session.getSSLParser()!=null && session.getSSLParser().handShakeDone){
					readsize = session.readSSLData(buffer);
				}
				else{
					readsize = session.read(buffer);
				}
				
				
				if (isRemoteClosed(readsize,buffer)) {
					if(byteOutputStream.size()!=0){
						return buffer;
					}
					return null;
				}
				//readsize为0时认为是超时
				else if(readsize==0){
					
					//使用消息划分器进行消息划分
					if(messageParter!=null && messageParter.canPartition(session,byteOutputStream.toByteArray(), elapsedtime)){
						break;
					}
					
					//超时判断
					if(readTimeout==elapsedtime){
						Logger.simple("Socket load timeout,return recived data.");
						break;
					}
					
					TEnv.sleep(1);
					//超时时间自增
					elapsedtime++;
				}
				else {
					//如果读到数据,则清零超时事件
					wroteCount+=readsize;
					byteOutputStream.write(buffer.array(), 0, readsize);
				}
			}
			if(byteOutputStream.size()!=0){
				buffer = ByteBuffer.wrap(byteOutputStream.toByteArray());
			}
		}
		//读取完毕,标志改为 false
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
