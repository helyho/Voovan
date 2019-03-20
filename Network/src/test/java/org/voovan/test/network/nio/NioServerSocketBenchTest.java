package org.voovan.test.network.nio;

import org.voovan.network.tcp.TcpServerSocket;
import org.voovan.test.network.ServerBenchHandlerTest;

import java.io.IOException;

public class NioServerSocketBenchTest {

	public static void main(String[] args) throws IOException {
		TcpServerSocket serverSocket = new TcpServerSocket("127.0.0.1",28080,30*1000);
		serverSocket.handler(new ServerBenchHandlerTest());
		serverSocket.start();
	}
}
