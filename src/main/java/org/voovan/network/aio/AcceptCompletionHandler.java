package org.voovan.network.aio;

import org.voovan.network.EventTrigger;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * Aio Accept 事件
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AioServerSocket>{

	private EventTrigger eventTrigger;
	public AcceptCompletionHandler(EventTrigger eventTrigger){
		this.eventTrigger = eventTrigger;
	}
	
	@Override
	public void completed(AsynchronousSocketChannel socketChannel, AioServerSocket serverSocket) {
		try {
			//接续接收 accept 请求
			serverSocket.catchAccept();
			
			AioSocket socket = new AioSocket(serverSocket,socketChannel);
			
			//触发 Accept 事件
			eventTrigger.fireAccept(socket.getSession());
			
			
		} catch (IOException e) {
			eventTrigger.fireException(e);
		}
	}

	@Override
	public void failed(Throwable exc, AioServerSocket attachment) {
		if(exc instanceof Exception){
			//触发 onException 事件
			eventTrigger.fireException(( Exception)exc);
		}
	}

}
