package org.voovan.test.network.nio;

import java.io.IOException;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.nio.NioServerSocket;
import org.voovan.test.network.ServerHandlerTest;

public class NioServerSocketTest  {

	public static void main(String[] args) throws IOException {
		NioServerSocket serverSocket = new NioServerSocket("127.0.0.1",2031,500);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.start();
	}
}
