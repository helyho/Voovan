package org.voovan.test.network.ssl;

import org.voovan.network.SSLManager;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.nio.NioServerSocket;
import org.voovan.test.network.ServerHandlerTest;

public class NioSSLServer  {

	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		String certFile = System.getProperty("user.dir")+"/Network/src/test/java/org/voovan/test/network/ssl/ssl_ks";
		sslManager.loadCertificate(certFile, "passStr","123123");
		
		NioServerSocket serverSocket = new NioServerSocket("127.0.0.1",2031,1000000,1);
		serverSocket.setSSLManager(sslManager);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.messageSplitter(new LineMessageSplitter());
		serverSocket.start();
	}
}
