package org.voovan.test.network.nio;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.nio.NioSocket;
import org.voovan.test.network.ClientHandlerTest;

public class NioSocketTest {
	
	public static void main(String[] args) throws Exception {
		NioSocket socket = new NioSocket("127.0.0.1",2031,500);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.start();
	}
}
