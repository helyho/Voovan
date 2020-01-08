package org.voovan.test.network.ssl;

import org.voovan.network.SSLManager;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.tcp.TcpServerSocket;
import org.voovan.test.network.ServerHandlerTest;

public class NioSSLServer  {

	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		String serverKeyFile = System.getProperty("user.dir")+"/Network/src/test/java/org/voovan/test/network/ssl/sslkeys/server.keystore";
		String clientTrustFile = System.getProperty("user.dir")+"/Network/src/test/java/org/voovan/test/network/ssl/sslkeys/trust_client.keystore";
		sslManager.loadKey(serverKeyFile, "123456","654321");
		sslManager.loadTrustKey(clientTrustFile, "123456");
		
		TcpServerSocket serverSocket = new TcpServerSocket("127.0.0.1",2031,1000000,1);
		serverSocket.setSSLManager(sslManager);
		serverSocket.handler(new ServerHandlerTest());
		serverSocket.filterChain().add(new StringFilter());
		serverSocket.messageSplitter(new LineMessageSplitter());
		serverSocket.start();
	}
}
