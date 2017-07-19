package org.voovan.test.network.ssl;

import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioSocket;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.test.network.ClientHandlerTest;
import org.voovan.tools.log.Logger;

public class AioSSLClient {
	
	public static void main(String[] args) throws Exception {
		SSLManager sslManager = new SSLManager("SSL");
		String certFile = System.getProperty("user.dir")+"/Network/src/test/java/org/voovan/test/network/ssl/ssl_ks";
		sslManager.loadCertificate(certFile, "passStr","123123");
		
		AioSocket socket = new AioSocket("127.0.0.1",2031,1000000,1);
		socket.setSSLManager(sslManager);
		socket.handler(new ClientHandlerTest());
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
		socket.start();
		Logger.simple("==================================Terminate==================================");

		//重连操作
		socket.restart();
		Logger.simple("==================================Terminate==================================");
		socket.getSession().restart();
	}
}
