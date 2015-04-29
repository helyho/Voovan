package org.voovan.test.network.ssl;

import org.voovan.network.SSLManager;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.nio.NioServerSocket;
import org.voovan.test.network.ServerHandlerTest;

public class NioSSLServer  {

	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		sslManager.loadCertificate(System.getProperty("user.dir")+"/src/test/java/org/voovan/test/network/ssl/ssl_ks", "passStr","123123");
		
		NioServerSocket serverSocket = new NioServerSocket("127.0.0.1",2031,500);
		serverSocket.setSSLManager(sslManager);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.start();
	}
}
