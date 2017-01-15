package org.voovan.network.aio;

import org.voovan.network.EventTrigger;
import org.voovan.network.MessageLoader;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

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
	private ByteBufferChannel byteBufferChannel;
	private AioSession session;
	
	public ReadCompletionHandler(AioSocket aioSocket, ByteBufferChannel byteBufferChannel){
		this.byteBufferChannel = byteBufferChannel;
		this.aioSocket = aioSocket;
		this.session = aioSocket.getSession();
	}
	
	

	@Override
	public void completed(Integer length, ByteBuffer buffer) {
		try {
			// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
			if (MessageLoader.isRemoteClosed(length, buffer) && session.isConnect()) {
				session.close();
			} else {
				buffer.flip();
			
				if (length > 0) {

					// 接收数据
					byteBufferChannel.writeEnd(buffer);

					// 触发 onReceive 事件
					EventTrigger.fireReceiveThread(session);
					
					// 接收完成后重置buffer对象
					buffer.clear();

					// 继续接收 Read 请求
					if(aioSocket.isConnect()) {
						aioSocket.catchRead(buffer);
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
			session.close();
			return;
		}

		if(exc instanceof Exception){
			//触发 onException 事件
			EventTrigger.fireExceptionThread(session, (Exception)exc);
		}
	}
}
