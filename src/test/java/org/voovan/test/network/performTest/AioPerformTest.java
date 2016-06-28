package org.voovan.test.network.performTest;

import org.voovan.http.server.HttpServerFilter;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.HttpMessageSplitter;

import java.io.IOException;

public class AioPerformTest  {

	public static void main(String[] args) throws IOException {
		AioServerSocket serverSocket = new AioServerSocket("0.0.0.0",28081,5000);
		serverSocket.handler(new PerformTestHandler());
		serverSocket.filterChain().add(new HttpServerFilter());
		serverSocket.messageSplitter(new HttpMessageSplitter());
		serverSocket.start();
	}
}
