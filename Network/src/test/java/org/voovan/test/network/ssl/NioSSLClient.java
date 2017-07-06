package org.voovan.test.network.ssl;

import org.voovan.network.SSLManager;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.nio.NioSocket;
import org.voovan.test.network.ClientHandlerTest;

public class NioSSLClient {
	
	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		String certFile = System.getProperty("user.dir")+"/Network/src/test/java/org/voovan/test/network/ssl/ssl_ks";
		sslManager.loadCertificate(certFile, "passStr","123123");
		
		NioSocket socket = new NioSocket("127.0.0.1",2031,1000000);
		socket.setSSLManager(sslManager);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
		socket.start();
	}
}
