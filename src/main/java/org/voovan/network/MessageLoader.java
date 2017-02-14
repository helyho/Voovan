package org.voovan.network;

import org.voovan.network.exception.SocketDisconnectByRemote;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;


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
	private StopType stopType;
	private ByteBufferChannel byteBufferChannel;
	private boolean isDirectRead;
	private int readZeroCount = 0;
	private int splitLength;
	/**
	 * 构造函数
	 * @param session Session 对象
	 */
	public MessageLoader(IoSession session) {
		this.session = session;
		stopType = StopType.RUNNING;
		isDirectRead = false;
		//准备缓冲流
		byteBufferChannel = session.getByteBufferChannel();
	}

	/**
	 * 是否是直接读取模式
	 * @return true 直接读取模式,false 常规过滤器读取模式
	 */
	public boolean isDirectRead() {
		return isDirectRead;
	}

	/**
	 * 设置是否启用直接读取模式
	 * @param directRead true 直接读取模式,false 常规过滤器读取模式
	 */
	public void setDirectRead(boolean directRead) {
		isDirectRead = directRead;
	}

	public enum StopType {
		RUNNING,SOCKET_CLOSE,STREAM_END,REMOTE_DISCONNECT,MSG_SPLITTER,EXCEPTION
	}

	/**
	 * 获取停止类型
	 * @return 停止类型
	 */
	public StopType getStopType() {
		return stopType;
	}

	/**
	 * 设置停止类型
	 * @param stopType 停止类型
	 */
	public void setStopType(StopType stopType) {
		this.stopType = stopType;
	}

	/**
	 * 判断连接是否意外断开
	 * @param buffer  缓冲区
	 * @param length  长度
	 * @return 是否意外断开
	 * @throws SocketDisconnectByRemote  Socket 断开异常
	 */
	public static boolean isRemoteClosed(ByteBuffer buffer, Integer length) throws SocketDisconnectByRemote{
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
	 * 判断连接是否意外断开
	 * @param buffer  缓冲区
	 * @param length  长度
	 * @return 是否意外断开
	 * @throws SocketDisconnectByRemote  Socket 断开异常
	 */
	public static boolean isRemoteClosed(byte[] buffer, Integer length) throws SocketDisconnectByRemote{
		if(length==-1){
			//触发 disconnect 事件
			return true;
		}
		//如果 buffer 被冲满,且起始、中位、结束的字节都是结束符(Ascii=4)则连接意外结束
		if(length>2
				&& buffer[0]==4 //起始判断
				&& buffer[length/2]==4 //中位判断
				&& buffer[length-1]==4){ //结束判断
			return true;
		}

		return false;
	}
	
	/**
	 * 读取 socket 中的数据
	 * 	逐字节读取数据,并用消息截断器判断消息包是否完整,消息粘包有两种截断方式:
	 * 	1.消息截断器生效
	 * 	2.消息读取时间超时,例如设置5m,则连续5秒内没有读取到有用的消息则返回报文.
	 * @return 读取的缓冲区数据
	 * @throws IOException IO 异常
	 */
	public ByteBuffer read() throws IOException {

		ByteBuffer result = null;
		int oldByteChannelSize = 0;

		ByteBufferChannel dataByteBuffer = null;

		if(session.getSSLParser()!=null && session.getSSLParser().handShakeDone) {
			dataByteBuffer = new ByteBufferChannel(session.sockContext().getBufferSize());
		}else{
			dataByteBuffer = session.getByteBufferChannel();
		}

		stopType = StopType.RUNNING;

		if(session==null){
			return null;
		}

		//获取消息分割器
		MessageSplitter messageSplitter = session.sockContext().messageSplitter();

		if(messageSplitter==null){
			Logger.error("[Error] MessageSplitter is null, you need to invoke SocketContext object's messageSplitter method to set MessageSplitter Object in it.");
			return null;
		}

		while (stopType==StopType.RUNNING && !isDirectRead) {

			//如果连接关闭,且读取缓冲区内没有数据时,退出循环
			if(!session.isConnected() && session.getByteBufferChannel().size()==0){
				stopType = StopType.SOCKET_CLOSE;
			}

			int readsize = byteBufferChannel.size() - oldByteChannelSize;

			//读出数据
//			if(session.getSSLParser()!=null && session.getSSLParser().handShakeDone){
//				readsize = session.readSSLData(tmpByteBuffer);
//			}else{
//				readsize = session.read(tmpByteBuffer);
//			}

			if(session.getSSLParser()!=null && session.getSSLParser().handShakeDone) {
				ByteBuffer sslData = ByteBuffer.allocate(session.sockContext().getBufferSize());
				session.readSSLData(sslData);
				dataByteBuffer.writeEnd(sslData);
			}

			//判断连接是否关闭
			if (isRemoteClosed(dataByteBuffer.getByteBuffer(), dataByteBuffer.size())) {
				stopType = StopType.REMOTE_DISCONNECT;
			}

			//使用消息划分器进行消息划分
			if(readsize==0) {
				splitLength = messageSplitter.canSplite(session,  dataByteBuffer.getByteBuffer());
				if (splitLength > 0) {
					stopType = StopType.MSG_SPLITTER ;
				}
			}

			//超时判断,防止读0时导致的高 CPU 负载
			if(readsize==0 && stopType == StopType.RUNNING){
				if(readZeroCount >= session.sockContext().getReadTimeout()){
					stopType = StopType.STREAM_END;
				}else {
					readZeroCount++;
					TEnv.sleep(1);
				}
			}

			oldByteChannelSize = byteBufferChannel.size();
		}

		//如果是消息截断器截断的消息则调用消息截断器处理的逻辑
		if(stopType==StopType.MSG_SPLITTER) {
			result = ByteBuffer.allocate(splitLength);
			dataByteBuffer.readHead(result);
		} else {
			result = ByteBuffer.allocate(0);
		}

		return result;
	}

	/**
	 * 直接读取缓冲区的数据
	 * @return 字节缓冲对象ByteBuffer
	 * @exception IOException IO异常
	 */
	public synchronized ByteBuffer directRead() throws IOException {
		ByteBuffer data = null;

		if(session.getSSLParser()!=null && session.getSSLParser().handShakeDone) {
			ByteBuffer sslData = ByteBuffer.allocate(session.sockContext().getBufferSize());
			session.readSSLData(sslData);
			data = sslData;
		}else {
			data = ByteBuffer.allocate(session.sockContext().getBufferSize());
			session.getByteBufferChannel().readHead(data);
		}

        if(!data.hasRemaining() && !session.isConnected()){
            return null;
        }

        return data;

	}
}
