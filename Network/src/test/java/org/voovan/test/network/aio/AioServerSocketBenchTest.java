package org.voovan.test.network.aio;

import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.test.network.ServerBenchHandlerTest;

import java.io.IOException;

public class AioServerSocketBenchTest {

	public static void main(String[] args) throws IOException {
		AioServerSocket serverSocket = new AioServerSocket("127.0.0.1",28080,30*1000);
		serverSocket.handler(new ServerBenchHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.messageSplitter(new LineMessageSplitter());
		serverSocket.start();
	}
}
