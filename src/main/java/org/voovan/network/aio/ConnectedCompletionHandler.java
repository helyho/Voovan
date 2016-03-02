package org.voovan.network.aio;

import java.nio.channels.CompletionHandler;

import org.voovan.network.EventTrigger;

/**
 * Aio 连接事件
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ConnectedCompletionHandler implements CompletionHandler<Void, AioSocket>{

	private EventTrigger eventTrigger;

	private boolean finished;
	public ConnectedCompletionHandler(EventTrigger eventTrigger){
		this.eventTrigger = eventTrigger;
		finished = false;
	}

	/**
	 * 获取是否完成 connect 操作
     */
	public synchronized boolean isFinished() {
		return finished;
	}

	@Override
	public void completed(Void arg1,  AioSocket socketContext) {
		// 不处理,这个方法原来是要触发 onConnect 事件,现在移到 AioSocket 的 start 方法触发。
		finished = true;

	}

	@Override
	public void failed(Throwable exc,  AioSocket socketContext) {
		if(exc instanceof Exception){
			//触发 onException 事件
			eventTrigger.fireException((Exception)exc);
		}
	}

}
