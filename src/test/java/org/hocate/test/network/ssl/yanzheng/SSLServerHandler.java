package org.hocate.test.network.ssl.yanzheng;

import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.network.SSLManager;
import org.hocate.network.SSLParser;

public class SSLServerHandler implements IoHandler {
	private SSLManager sslManager;

	public SSLServerHandler(){
		try{
		sslManager = new SSLManager("TLS");
		sslManager.loadCertificate("/Users/helyho/Work/Java/BuizPlatform/src/test/java/org/hocate/test/network/ssl/ssl_ks", "passStr","123123");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public Object onConnect(IoSession session) {
		try {
			SSLParser sslParser = sslManager.createServerSSLParser(session);
			System.out.println(sslParser.doHandShake());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {

	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		System.out.println("\r\n=======IoHandler recive======\r\n");
		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
	}

	@Override
	public void onException(IoSession session, Exception e) {
	}

}
