package org.voovan.test.network;


import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SocketDisconnectByRemote;
import org.voovan.tools.log.Logger;

public class ServerBenchHandlerTest implements IoHandler {

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
					"Server: Voovan-WebServer/V1.0-RC-1\r\n" +
					"Connection: keep-alive\r\n" +
					"Content-Length: 2\r\n" +
					"Date: Thu, 05 Jan 2017 04:55:20 GMT\r\n" +
					"Content-Type: text/html\r\n"+
					"\r\n"+
					"OK\r\n\n";
		}

		@Override
		public void onException(IoSession session, Exception e) {
			if(e instanceof SocketDisconnectByRemote){
				Logger.simple("Connection disconnect by client");
			}else {
				Logger.error("Server Exception:",e);
		}
		session.close();
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		session.close();
	}

}
