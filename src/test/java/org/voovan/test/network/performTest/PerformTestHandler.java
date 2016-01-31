package org.voovan.test.network.performTest;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.log.Logger;

public class PerformTestHandler implements IoHandler {

	private String responseStr ;
	
	public PerformTestHandler(){
		responseStr  =	"GET / HTTP/1.1\r\n"+
						"Host: www.baidu.com\r\n"+
						"Connection: keep-alive\r\n"+
						"Pragma: no-cache\r\n"+
						"Cache-Control: no-cache\r\n"+
						"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\r\n\r\n";

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
		Logger.error(e.getMessage());
	}

	@Override
	public void onSent(IoSession session, Object obj) {
//		TEnv.sleep(2);
		session.close();
	}

}
