package org.hocate.test.network.nio;

import java.io.IOException;

import org.hocate.network.filter.StringFilter;
import org.hocate.network.nio.NioServerSocket;
import org.hocate.test.network.ServerHandlerTest;

public class NioServerSocketTest  {

	public static void main(String[] args) throws IOException {
		NioServerSocket serverSocket = new NioServerSocket("0.0.0.0",2031,500);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.start();
	}
}
