package org.hocate.test.network.ssl;

import org.hocate.network.SSLManager;
import org.hocate.network.filter.StringFilter;
import org.hocate.network.nio.NioSocket;
import org.hocate.test.network.ClientHandlerTest;

public class NioSSLClient {
	
	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		sslManager.loadCertificate("/Users/helyho/Work/Java/MyPlatform/src/test/java/org/hocate/test/network/ssl/ssl_ks", "passStr","123123");
		
		NioSocket socket = new NioSocket("0.0.0.0",2031,500);
		socket.setSSLManager(sslManager);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.start();
	}
}
