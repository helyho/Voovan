package org.voovan.test.network;


import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.log.Logger;

public class ServerHttpHandlerTest implements IoHandler {

	@Override
	public Object onConnect(IoSession session) {
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
        return "HTTP/1.1 200 OK\r\n" +
				"Content-Length: 2\r\n" +
				"\r\n" +
				"OK\r\n\r\n";
	}

	@Override
    public void onException(IoSession session, Exception e) {
        Logger.error("Server Exception:",e);
		session.close();
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		session.close();
	}

}
