package org.voovan.test.network.performTest;

import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.filter.StringFilter;

import java.io.IOException;

public class AioPerformTest  {

	public static void main(String[] args) throws IOException {
		AioServerSocket serverSocket = new AioServerSocket("0.0.0.0",28081,5000);
		serverSocket.handler(new PerformTestHandler());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.messageSplitter(new PerformTestSpliter());
		serverSocket.start();
	}
}
