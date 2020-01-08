package org.voovan.test.network.ssl;

import org.voovan.network.SSLManager;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.test.network.ClientHandlerTest;
import org.voovan.tools.log.Logger;

public class NioSSLClient {
	
	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		String clientKeyFile = System.getProperty("user.dir")+"/Network/src/test/java/org/voovan/test/network/ssl/sslkeys/client.keystore";
		String serverTrustFile = System.getProperty("user.dir")+"/Network/src/test/java/org/voovan/test/network/ssl/sslkeys/trust_server.keystore";
		sslManager.loadKey(clientKeyFile, "123456","654321");
		sslManager.loadTrustKey(serverTrustFile, "123456");

		TcpSocket socket = new TcpSocket("127.0.0.1",2031,1000000,1);
		socket.setSSLManager(sslManager);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
		socket.start();
	}
}
