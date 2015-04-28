package org.hocate.network.aio.completionhandler;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import org.hocate.network.EventTrigger;
import org.hocate.network.aio.AioSocket;
import org.hocate.tools.log.Logger;

/**
 * Aio 连接事件
 * @author helyho
 *
 */
public class ConnectedCompletionHandler implements CompletionHandler<Void, AioSocket>{

	private EventTrigger eventTrigger;
	public ConnectedCompletionHandler(EventTrigger eventTrigger){
		this.eventTrigger = eventTrigger;
	}
	
	@Override
	public void completed(Void arg1,  AioSocket socketContext) {
		try{
			//触发 Connect 事件
			eventTrigger.fireConnect();
			socketContext.catchRead(ByteBuffer.allocate(1024));
		}
		catch(Exception e){
			Logger.error("Class ConnectedCompletionHandler Error:"+e.getMessage());
			eventTrigger.fireException(e);
		}
	}

	@Override
	public void failed(Throwable exc,  AioSocket socketContext) {
		if(exc instanceof Exception){
			Logger.error("Error: Aio connected socket error!");
			//触发 onException 事件
			eventTrigger.fireException(new Exception(exc));
		}
	}

}
