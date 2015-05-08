package org.voovan.test.network;

import java.nio.ByteBuffer;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.MessageLoader;
import org.voovan.tools.log.Logger;

public class ClientHandlerTest implements IoHandler {

	@Override
	public Object onConnect(IoSession session) {
		Logger.simple("onConnect");
		session.setAttribute("key", "attribute value");
		String msg = new String("test message");
		return msg;
	}

	@Override
	public void onDisconnect(IoSession session) {
		Logger.simple("onDisconnect");
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		//+"["+session.remoteAddress()+":"+session.remotePort()+"]"
		Logger.simple("Client onRecive: "+obj.toString());
		Logger.simple("Attribute onRecive: "+session.getAttribute("key"));
		session.close();
		return obj;
	}

	@Override
	public void onException(IoSession session, Exception e) {
		Logger.simple("Client Exception");
		Logger.error(e);
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		ByteBuffer sad = (ByteBuffer)obj;
		sad = (ByteBuffer)sad.rewind();
		Logger.simple("Client onSent: "+MessageLoader.byteBufferToString(sad));
	}

}
