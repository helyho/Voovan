package org.voovan.network;

import org.voovan.network.messagesplitter.TransferSplitter;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.exception.MemoryReleasedException;
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
	private boolean enable;

	/**
	 * 构造函数
	 * @param session Session 对象
	 */
	public MessageLoader(IoSession session) {
		this.session = session;
		enable = true;
		//准备缓冲流
		byteBufferChannel = session.getReadByteBufferChannel();
	}

	/**
	 * 获取是否使用分割器读取
	 *
	 * @return true 使用分割器读取,false 不使用分割器读取,且不会出发 onRecive 事件
	 */
	public boolean isEnable() {
		return enable;
	}

	/**
	 * 设置是否是否使用分割器读取
	 * @param enable true 使用分割器读取,false 不使用分割器读取,且不会出发 onRecive 事件
	 */
	public void enable(boolean enable) {
		this.enable = enable;
	}

	public enum StopType {
		RUNNING,
		SOCKET_CLOSED,
		STREAM_END,
		MSG_SPLITTER,
		EXCEPTION
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
	 * 判断字节所属流是否结束
	 * @param buffer  缓冲区
	 * @param length  长度
	 * @return 是否意外断开
	 */
	public static boolean isStreamEnd(ByteBuffer buffer, Integer length) {
//		length==-1时流结束
		if(length==-1){
			return true;
		}

		return false;
	}

	/**
	 * 判断字节所属流是否结束
	 * @param buffer  缓冲区
	 * @param length  长度
	 * @return 是否意外断开
	 */
	public static boolean isStreamEnd(byte[] buffer, Integer length) {

		if(length==-1){
			return true;
		}

		return false;
	}

	/**
	 * 关闭 MessageLoader
	 */
	public void close(){
		stopType = StopType.SOCKET_CLOSED;
	}


	/**
	 * 读取 socket 中的数据
	 * 	逐字节读取数据,并用消息截断器判断消息包是否完整,消息粘包有两种截断方式:
	 * 	1.消息截断器生效
	 * 	2.消息读取时间超时,例如设置5m,则连续5秒内没有读取到有用的消息则返回报文.
	 * @return 读取的缓冲区数据
	 * @throws IOException IO 异常
	 */
	public int read() throws IOException {
		int readZeroCount = 0;
		int splitLength = 0;

		ByteBuffer result = TByteBuffer.EMPTY_BYTE_BUFFER;
		int oldByteChannelSize = 0;

		ByteBufferChannel dataByteBufferChannel = null;
		ByteBuffer dataByteBuffer = null;

		dataByteBufferChannel = session.getReadByteBufferChannel();

		stopType = StopType.RUNNING;

		if(session==null){
			return -1;
		}

		//获取消息分割器
		MessageSplitter messageSplitter = session.socketContext().messageSplitter();

		if(messageSplitter==null){
			Logger.error("[Error] MessageSplitter is null, you need to invoke SocketContext object's messageSplitter method to set MessageSplitter Object in it.");
			return -1;
		}

		boolean isConnect = true;

		while ( isConnect && enable &&
				(stopType== StopType.RUNNING ) &&
				dataByteBufferChannel.size() > 0
		) {

			if(session.socketContext() instanceof UdpSocket) {
				isConnect = session.isOpen();
			}else {
				isConnect = session.isConnected();
			}

			//如果连接关闭,
			if(!isConnect){
				stopType = StopType.SOCKET_CLOSED;
				return -1;
			}

			int readsize = byteBufferChannel.size() - oldByteChannelSize;

			try {
				dataByteBuffer = dataByteBufferChannel.getByteBuffer();
				try {

					//判断连接是否关闭
					if (isStreamEnd(dataByteBuffer, dataByteBufferChannel.size())) {
						stopType = StopType.STREAM_END;
					}

					//使用消息划分器进行消息划分
					if (readsize == 0 && dataByteBuffer.limit() > 0) {
						if (messageSplitter instanceof TransferSplitter) {
							splitLength = dataByteBuffer.limit();
						} else {
							splitLength = messageSplitter.canSplite(session, dataByteBuffer);
						}

						if (splitLength >= 0) {
							stopType = StopType.MSG_SPLITTER;
						}
					}
				} finally {
					dataByteBufferChannel.compact();
				}
			}catch(MemoryReleasedException e){
				stopType = StopType.SOCKET_CLOSED;
			}

			//超时判断,防止读0时导致的高 CPU 负载
			if( readsize==0 && stopType == StopType.RUNNING ){
				if(readZeroCount >= session.socketContext().getReadTimeout()){
					stopType = StopType.STREAM_END;
				}else {
					readZeroCount++;
					HeartBeat.interceptHeartBeat(session, dataByteBufferChannel);
					TEnv.sleep(1);
				}
			}else{
				readZeroCount = 0;
			}

			oldByteChannelSize = byteBufferChannel.size();
		}

		//如果是流结束,对方关闭,本地关闭这三种情况则返回 null
		// 返回是 null 则在EventProcess中直接返回,不做任何处理
		if (stopType == StopType.STREAM_END ||
				stopType == StopType.SOCKET_CLOSED) {
			result = null;
		}

		//如果是消息截断器截断的消息则调用消息截断器处理的逻辑
		else if (stopType == StopType.MSG_SPLITTER) {
			if (splitLength >= 0) {
				return splitLength;
			} else {
				return -1;
			}
		} else {
			return -1;
		}

		return -1;
	}
}
