package org.hocate.test.network.aio;

import org.hocate.network.aio.AioSocket;
import org.hocate.network.filter.StringFilter;
import org.hocate.test.network.ClientHandlerTest;

public class AioSocketTest {
	
	public static void main(String[] args) throws Exception {
		AioSocket socket = new AioSocket("0.0.0.0",2031,100);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.start();
	}
}
