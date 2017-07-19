package org.voovan.test.network.aio;

import org.voovan.network.aio.AioSocket;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.test.network.ClientHandlerTest;
import org.voovan.tools.log.Logger;

public class AioSocketTest {
	
	public static void main(String[] args) throws Exception {
		AioSocket socket = new AioSocket("127.0.0.1",2031,5000, 1);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
		socket.start();
		Logger.simple("==================================Terminate==================================");

		//重连操作
		socket.restart();
		Logger.simple("==================================Terminate==================================");
		socket.getSession().restart();
	}
}
