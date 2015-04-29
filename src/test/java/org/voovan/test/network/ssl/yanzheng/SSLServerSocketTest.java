package org.voovan.test.network.ssl.yanzheng;

import java.io.IOException;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.nio.NioServerSocket;

public class SSLServerSocketTest  {

	public static void main(String[] args) throws IOException {
		NioServerSocket serverSocket = new NioServerSocket("0.0.0.0",2031,500);
		serverSocket.handler(new SSLServerHandler());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.start();
	}
}
