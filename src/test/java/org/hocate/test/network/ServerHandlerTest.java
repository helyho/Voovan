package org.hocate.test.network;


import java.nio.ByteBuffer;

import org.hocate.log.Logger;
import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.network.MessageLoader;

public class ServerHandlerTest implements IoHandler {

	@Override
	public Object onConnect(IoSession session) {
		Logger.simple("onConnect");
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {
		Logger.simple("onDisconnect");
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		Logger.simple("Server onRecive: "+obj.toString());
		return "===="+obj+" ===== ";
	}

	@Override
	public void onException(IoSession session, Exception e) {
		Logger.simple("Server Exception:");
		e.printStackTrace();
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		ByteBuffer sad = (ByteBuffer)obj;
		sad = (ByteBuffer)sad.rewind();
		Logger.simple("Server onSent: "+MessageLoader.byteBufferToString(sad));
	}

}
