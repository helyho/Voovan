package org.voovan.test.network.nio;

import org.voovan.network.nio.NioServerSocket;
import org.voovan.test.network.ServerBenchHandlerTest;

import java.io.IOException;

public class NioServerSocketBenchTest {

	public static void main(String[] args) throws IOException {
		NioServerSocket serverSocket = new NioServerSocket("127.0.0.1",28080,30*1000);
		serverSocket.handler(new ServerBenchHandlerTest());
		serverSocket.start();
	}
}
