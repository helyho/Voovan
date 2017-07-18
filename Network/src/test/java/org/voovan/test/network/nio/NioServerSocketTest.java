package org.voovan.test.network.nio;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.nio.NioServerSocket;
import org.voovan.test.network.ServerHandlerTest;

import java.io.IOException;

public class NioServerSocketTest  {

	public static void main(String[] args) throws IOException {
		NioServerSocket serverSocket = new NioServerSocket("127.0.0.1",2031,5000, 1);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.messageSplitter(new LineMessageSplitter());
		serverSocket.start();
	}
}
