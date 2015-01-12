package org.hocate.test.network.ssl;

import org.hocate.network.SSLManager;
import org.hocate.network.aio.AioServerSocket;
import org.hocate.network.filter.StringFilter;
import org.hocate.test.network.ServerHandlerTest;

public class AioSSLServer  {

	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		sslManager.loadCertificate("/Users/helyho/Work/Java/MyPlatform/src/test/java/org/hocate/test/network/ssl/ssl_ks", "passStr","123123");
		
		AioServerSocket serverSocket = new AioServerSocket("0.0.0.0",2031,500);
		serverSocket.setSSLManager(sslManager);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.start();
	}
}
