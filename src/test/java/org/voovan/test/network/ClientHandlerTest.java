package org.voovan.test.network;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

public class ClientHandlerTest implements IoHandler {

	@Override
	public Object onConnect(IoSession session) {
		Logger.simple("onConnect");
		session.setAttribute("key", "attribute value");
		String msg = new String("test message\r\n");
		return msg;
	}

	@Override
	public void onDisconnect(IoSession session) {
		Logger.simple("onDisconnect");
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		Logger.simple(session.remoteAddress()+":"+session.remotePort());
		//+"["+session.remoteAddress()+":"+session.remotePort()+"]"
		Logger.simple("Client onRecive: "+obj.toString());
		Logger.simple("Attribute onRecive: "+session.getAttribute("key"));
		session.close();
		return null;
	}

	@Override
	public void onException(IoSession session, Exception e) {
		Logger.simple("Client Exception");
		Logger.error(e);
		session.close();
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		ByteBuffer sad = (ByteBuffer)obj;
		sad = (ByteBuffer)sad.rewind();
		Logger.simple("Client onSent: "+new String(sad.array()));
	}

}
