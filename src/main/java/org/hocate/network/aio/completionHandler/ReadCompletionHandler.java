package org.hocate.network.aio.completionHandler;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.CompletionHandler;

import org.hocate.network.ByteBufferChannel;
import org.hocate.network.EventTrigger;
import org.hocate.network.MessageLoader;
import org.hocate.network.aio.AioSession;
import org.hocate.network.aio.AioSocket;
import org.hocate.tools.TEnv;
import org.hocate.tools.TObject;

/**
 * Aio 读取事件
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
	public void completed(Integer length,  ByteBuffer buffer) {
		
		//如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
		if(MessageLoader.isRemoteClosed(length,buffer) && session.isConnect()){
			session.close();
		}
		else{	
			buffer.flip();
			try {
				if(length>0){
					
					//接收数据
					byteBufferChannel.write(buffer);
					
					//触发 onReceive 事件
					eventTrigger.fireReceiveThread();
					//接收完成后重置 buffer;
					buffer.clear();
					
					//下一次监听延迟1毫秒,用于给 event 的状态改变的时间
					TEnv.sleep(1);
					
					//继续接收 Read 请求
					socket.catchRead(buffer);	
				}
			} catch (Exception e) {
				//触发 onException 事件
				eventTrigger.fireException(e);
			}
		}
	}

	@Override
	public void failed(Throwable exc,  ByteBuffer buffer) {
		if(exc instanceof Exception && !(exc instanceof AsynchronousCloseException)){
			//触发 onException 事件
			eventTrigger.fireException(TObject.cast(exc));
		}
	}
}
