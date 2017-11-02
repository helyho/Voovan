package org.voovan.network.aio;

import org.voovan.Global;
import org.voovan.network.EventTrigger;
import org.voovan.network.HeartBeat;
import org.voovan.network.MessageLoader;
import org.voovan.tools.ByteBufferChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.CompletionHandler;

/**
 * Aio 读取事件
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ReadCompletionHandler implements CompletionHandler<Integer,  ByteBuffer>{
	private AioSocket aioSocket;
	private ByteBufferChannel netByteBufferChannel;
	private ByteBufferChannel appByteBufferChannel;
	private AioSession session;

	public ReadCompletionHandler(AioSocket aioSocket, ByteBufferChannel byteBufferChannel){
		this.aioSocket = aioSocket;
		this.appByteBufferChannel = byteBufferChannel;
		this.session = aioSocket.getSession();
	}

	@Override
	public void completed(Integer length, ByteBuffer readTempBuffer) {
		try {

			if(netByteBufferChannel== null && session.getSSLParser()!=null) {
				netByteBufferChannel = new ByteBufferChannel(session.socketContext().getBufferSize());
			}

			// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
			if (MessageLoader.isStreamEnd(readTempBuffer, length) || !session.isConnected()) {
				session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
				session.close();
			} else {
				readTempBuffer.flip();

				if (length > 0) {

					if(session.getHeartBeat()!=null) {
						session.getMessageLoader().pause();
					}

					// 接收数据
					if(session.getSSLParser()!=null && session.getSSLParser().isHandShakeDone()){
						netByteBufferChannel.writeEnd(readTempBuffer);
						session.getSSLParser().unWarpByteBufferChannel(session, netByteBufferChannel, appByteBufferChannel);
					}else{
						appByteBufferChannel.writeEnd(readTempBuffer);
					}

					//检查心跳
					HeartBeat.interceptHeartBeat(session, appByteBufferChannel);

					if(session.getHeartBeat()!=null) {
						session.getMessageLoader().unpause();
					}

					if(appByteBufferChannel.size() > 0) {
						// 触发 onReceive 事件
						EventTrigger.fireReceiveThread(session);
					}

					// 接收完成后重置buffer对象
					readTempBuffer.clear();

					// 继续接收 Read 请求
					if(aioSocket.isConnected()) {
						Global.getThreadPool().execute(new Runnable() {
							@Override
							public void run() {
								aioSocket.catchRead(readTempBuffer);
							}
						});
					}
				}
			}
		} catch (IOException e) {
			// 触发 onException 事件
			session.getMessageLoader().setStopType(MessageLoader.StopType.EXCEPTION);
			EventTrigger.fireExceptionThread(session, e);
		}

	}

	@Override
	public void failed(Throwable exc,  ByteBuffer buffer) {
		if((exc instanceof AsynchronousCloseException) ||
				(exc instanceof ClosedChannelException)){
			return;
		}

		if(exc instanceof Exception){

			Exception e = (Exception)exc;

			//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
			if(e.getStackTrace()[0].getClassName().contains("sun.nio.ch")){
				session.close();
				return;
			}

			//触发 onException 事件
			EventTrigger.fireExceptionThread(session, (Exception)exc);
		}
	}

	public void release(){
		if(netByteBufferChannel!=null) {
			netByteBufferChannel.release();
		}
	}
}
