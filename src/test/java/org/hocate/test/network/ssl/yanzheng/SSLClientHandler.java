package org.hocate.test.network.ssl.yanzheng;

import org.hocate.log.Logger;
import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.network.SSLManager;
import org.hocate.network.SSLParser;

public class SSLClientHandler implements IoHandler {
	private SSLManager sslManager;
	
	public SSLClientHandler(){
		try{
		sslManager = new SSLManager("TLS");
		sslManager.loadCertificate("/Users/helyho/Work/Java/MyPlatform/src/test/java/org/hocate/test/network/ssl/ssl_ks", "passStr","123123");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@Override
	public Object onConnect(IoSession session) {
		try {
			SSLParser sslParser = sslManager.createClientSSLParser(session);
			Logger.simple("SSLManager created");
			Logger.simple(sslParser.doHandShake());
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
		Logger.simple("\r\n=======IoHandler recive======\r\n");
		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
	}

	@Override
	public void onException(IoSession session, Exception e) {
	}

}
