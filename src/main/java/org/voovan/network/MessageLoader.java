package org.voovan.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.network.exception.SocketDisconnectByRemote;
import org.voovan.tools.TEnv;


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
	private boolean isLoading;

	/**
	 * 构造函数
	 * @param session
	 * @param readTimeOut 超时时间
	 */
	public MessageLoader(IoSession session) {
		this.session = session;
		isLoading = false;
	}
	
	/**
	 * 停止读取
	 */
	public void stopLoading(){
		isLoading = false;
	}

	/**
	 * 判断连接是否意外断开
	 * @param length
	 * @param buffer
	 * @return
	 * @throws SocketDisconnectByRemote 
	 */
	public static boolean isRemoteClosed(Integer length,  ByteBuffer buffer) throws SocketDisconnectByRemote{
		if(length==-1){
			//触发 disconnect 事件
			throw new SocketDisconnectByRemote("Disconnect by Remote");
		}
		//如果 buffer 被冲满,且起始、中位、结束的字节都是结束符(Ascii=4)则连接意外结束
	   if(length>2
				&& buffer.get(0)==4 //起始判断
				&& buffer.get(length/2)==4 //中位判断 
				&& buffer.get(length-1)==4){ //结束判断 
		   	throw new SocketDisconnectByRemote("Disconnect by Remote");
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
		
		//获取消息分割器
		MessageSplitter messageSplitter = session.sockContext().messageSplitter();
		
		//准备缓冲流
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		
		//缓冲区字段,一次读一个字节,所以这里分配一个
		ByteBuffer oneByteBuffer = ByteBuffer.allocate(1);
		
		if (session != null) {
			isLoading = true;
			
			while (isLoading) {
				//如果连接关闭,退出循环
				if(!session.isConnect()){
					stopLoading();
				}
				
				oneByteBuffer.clear();
				int readsize = 0;
				
				//读出数据
				if(session.getSSLParser()!=null && session.getSSLParser().handShakeDone){
					readsize = session.readSSLData(oneByteBuffer);
				}else{
					readsize = session.read(oneByteBuffer);
				}
				
				if(readsize==0){
					TEnv.sleep(1);
				}
				
				//通道关闭,退出循环
				if(readsize==-1){
					stopLoading();
				}
				
				//将读出的数据写入缓冲区
				if(readsize>0) {
					byteOutputStream.write(oneByteBuffer.array(), 0, readsize);
				}
				
				
				//判断连接是否关闭
				if (isRemoteClosed(readsize,oneByteBuffer)) {
					stopLoading();
				}
				
				//使用消息划分器进行消息划分
				else if(messageSplitter!=null && readsize==1 && messageSplitter.canSplite(session,byteOutputStream.toByteArray())){
					stopLoading();
				}
			}
		}
		
		ByteBuffer retBuffer = null;
		
		if(byteOutputStream!=null){
			retBuffer = ByteBuffer.wrap(byteOutputStream.toByteArray());
		}
		
		return retBuffer;
	}

}
