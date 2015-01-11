package org.hocate.network.aio.completionHandler;

import java.nio.channels.CompletionHandler;

import org.hocate.network.EventTrigger;
import org.hocate.tools.TObject;

/**
 * Aio 连接事件
 * @author helyho
 *
 */
public class ConnectedCompletionHandler implements CompletionHandler<Void, Void>{

	private EventTrigger eventTrigger;
	public ConnectedCompletionHandler(EventTrigger eventTrigger){
		this.eventTrigger = eventTrigger;
	}
	
	@Override
	public void completed(Void arg1,  Void arg2) {
		try{
			//触发 Connect 事件
			eventTrigger.fireConnect();
		}
		catch(Exception e){
			eventTrigger.fireException(e);
		}
	}

	@Override
	public void failed(Throwable exc,  Void arg1) {
		if(exc instanceof Exception){
			//触发 onException 事件
			eventTrigger.fireException(TObject.cast(exc));
		}
	}

}
