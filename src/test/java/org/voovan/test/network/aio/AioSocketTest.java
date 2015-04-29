package org.voovan.test.network.aio;

import org.voovan.network.aio.AioSocket;
import org.voovan.network.filter.StringFilter;
import org.voovan.test.network.ClientHandlerTest;
import org.voovan.tools.log.Logger;

public class AioSocketTest {
	
	public static void main(String[] args) throws Exception {
		AioSocket socket = new AioSocket("127.0.0.1",2031,100);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.start();
		Logger.simple("Terminate");
	}
}
