package org.voovan.network.aio;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import org.voovan.network.EventTrigger;
import org.voovan.tools.log.Logger;

/**
 * Aio Accept 事件
 * 
 * @author helyho
 *
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
			Logger.error("Class AcceptCompletionHandler Error:"+e.getMessage());
			eventTrigger.fireException(e);
		}
	}

	@Override
	public void failed(Throwable exc, AioServerSocket attachment) {
		if(exc instanceof Exception){
			Logger.error("Error: Aio accept socket error!");
			//触发 onException 事件
			eventTrigger.fireException(new Exception(exc));
			
		}
	}

}
