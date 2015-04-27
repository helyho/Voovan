package org.hocate.test.network.aio;

import java.io.IOException;

import org.hocate.network.aio.AioServerSocket;
import org.hocate.network.filter.StringFilter;
import org.hocate.test.network.ServerHandlerTest;

public class AioServerSocketTest  {

	public static void main(String[] args) throws IOException {
		AioServerSocket serverSocket = new AioServerSocket("0.0.0.0",2031,1);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.start();
	}
}
