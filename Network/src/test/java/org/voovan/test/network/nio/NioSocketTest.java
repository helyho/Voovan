package org.voovan.test.network.nio;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.nio.NioSocket;
import org.voovan.test.network.ClientHandlerTest;
import org.voovan.tools.log.Logger;

public class NioSocketTest {
	
	public static void main(String[] args) throws Exception {
		NioSocket socket = new NioSocket("127.0.0.1",2031,500, 1);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
		socket.start();

		Logger.simple("Terminate");

		//重连操作
		socket.reStart();
	}
}
