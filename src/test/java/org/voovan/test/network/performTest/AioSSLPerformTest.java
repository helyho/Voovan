package org.voovan.test.network.performTest;

import java.io.IOException;

import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.filter.StringFilter;

public class AioSSLPerformTest  {

	public static void main(String[] args) throws IOException {
		SSLManager sslManager = new SSLManager("TLS",false);
		sslManager.loadCertificate(System.getProperty("user.dir")+"/src/test/java/org/voovan/test/network/ssl/ssl_ks", "passStr","123123");
		
		AioServerSocket serverSocket = new AioServerSocket("0.0.0.0",28081,5000);
		serverSocket.setSSLManager(sslManager);
		serverSocket.handler(new PerformTestHandler());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.messageSplitter(new PerformTestSpliter());
		serverSocket.start();
	}
}
