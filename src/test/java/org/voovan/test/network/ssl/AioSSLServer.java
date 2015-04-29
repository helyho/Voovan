package org.voovan.test.network.ssl;

import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.filter.StringFilter;
import org.voovan.test.network.ServerHandlerTest;

public class AioSSLServer  {

	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		sslManager.loadCertificate("/Users/helyho/Work/Java/MyPlatform/src/test/java/org/hocate/test/network/ssl/ssl_ks", "passStr","123123");
		
		AioServerSocket serverSocket = new AioServerSocket("127.0.0.1",2031,500);
		serverSocket.setSSLManager(sslManager);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.start();
	}
}
