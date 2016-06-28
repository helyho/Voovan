package org.voovan.test.network.performTest;

import org.voovan.http.message.Response;
import org.voovan.http.server.HttpResponse;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SocketDisconnectByRemote;
import org.voovan.tools.log.Logger;

public class PerformTestHandler implements IoHandler {

	private Response response ;
	
	public PerformTestHandler(){
		response = new Response();
		response.protocol().setStatus(200);
		response.body().write("OK");
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
		return response;
	}

	@Override
	public void onException(IoSession session, Exception e) {
		if(!(e instanceof SocketDisconnectByRemote)) {
			Logger.error(e.getMessage(), e);
		}
	}

	@Override
	public void onSent(IoSession session, Object obj) {
//		TEnv.sleep(2);
		session.close();
	}

}
