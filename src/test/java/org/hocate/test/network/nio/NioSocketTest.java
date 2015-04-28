package org.hocate.test.network.nio;

import org.hocate.network.filter.StringFilter;
import org.hocate.network.nio.NioSocket;
import org.hocate.test.network.ClientHandlerTest;

public class NioSocketTest {
	
	public static void main(String[] args) throws Exception {
		NioSocket socket = new NioSocket("127.0.0.1",2031,500);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.start();
	}
}
