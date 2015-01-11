package org.hocate.test.network.ssl.yanzheng;

import org.hocate.network.filter.StringFilter;
import org.hocate.network.nio.NioSocket;

public class SSLSocketTest {
	
	public static void main(String[] args) throws Exception {
		NioSocket socket = new NioSocket("0.0.0.0",2031,500);
//		socket.handler(new ClientHandlerTest());
		socket.handler(new SSLClientHandler());
		socket.filterChain().add(new StringFilter());
		socket.start();
	}
}
