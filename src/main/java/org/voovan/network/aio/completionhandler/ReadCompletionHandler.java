package org.voovan.network.aio.completionhandler;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;

import org.voovan.network.EventTrigger;
import org.voovan.network.MessageLoader;
import org.voovan.network.aio.AioSession;
import org.voovan.network.aio.AioSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

/**
 * Aio 读取事件
 * 
 * @author helyho
 *
 */
public class ReadCompletionHandler implements CompletionHandler<Integer,  ByteBuffer>{
	private AioSocket socket;
	private EventTrigger eventTrigger;
	private ByteBufferChannel byteBufferChannel;
	private AioSession session;
	
	public ReadCompletionHandler(EventTrigger eventTrigger,ByteBufferChannel byteBufferChannel){
		this.eventTrigger = eventTrigger;
		this.byteBufferChannel = byteBufferChannel;
		this.socket = (AioSocket)eventTrigger.getSession().sockContext();
		this.session = TObject.cast(eventTrigger.getSession());
	}
	
	

	@Override
	public void completed(Integer length, ByteBuffer buffer) {

		// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
		if (MessageLoader.isRemoteClosed(length, buffer) && session.isConnect()) {
			session.close();
		} else {
			buffer.flip();
			try {
				if (length > 0) {

					// 接收数据
					byteBufferChannel.write(buffer);

					// 触发 onReceive 事件
					eventTrigger.fireReceiveThread();
					
					// 接收完成后重置buffer对象;
					buffer.clear();

					// 继续接收 Read 请求
					socket.catchRead(buffer);
				}
			} catch (Exception e) {
				Logger.error("Class ReadCompletionHandler Error:"+e.getMessage());
				// 触发 onException 事件
				eventTrigger.fireException(e);
				
			}
		}
	}

	@Override
	public void failed(Throwable exc,  ByteBuffer buffer) {
		if(exc instanceof Exception && !(exc instanceof AsynchronousCloseException)){
			Logger.error("Error: Aio read socket error!");
			//触发 onException 事件
			eventTrigger.fireException(new Exception(exc));
		}
	}
}
