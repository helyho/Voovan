package org.voovan.test.network.performTest;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.log.Logger;

public class PerformTestHandler implements IoHandler {

	private String responseStr ;
	
	public PerformTestHandler(){
		responseStr  =	"HTTP/1.1 200 OK\n" +
						"Date: Wed, 10 Jun 2009 11:22:58 GMT\n" + 
						"Server: Microsoft-IIS/6.0\n" + 
						"X-Powered-By: ASP.NET\n" + 
						"Content-Length: 2\n" + 
						"Content-Type: text/html\n" + 
						"Cache-control: private\n\n"+
						"OK";
	}
	
	@Override
	public Object onConnect(IoSession session) {
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		return responseStr;
	}

	@Override
	public void onException(IoSession session, Exception e) {
		Logger.error(e.getMessage(),e);
	}

	@Override
	public void onSent(IoSession session, Object obj) {
//		TEnv.sleep(2);
		session.close();
	}

}
