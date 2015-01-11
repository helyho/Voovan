package org.hocate.test.network;


import java.nio.ByteBuffer;

import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.network.MessageLoader;

public class ServerHandlerTest implements IoHandler {

	@Override
	public Object onConnect(IoSession session) {
		System.out.println("onConnect");
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {
		System.out.println("onDisconnect");
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		System.out.println("Server onRecive: "+obj.toString());
		return "===="+obj+" ===== ";
	}

	@Override
	public void onException(IoSession session, Exception e) {
		System.out.print("Server Exception:");
		e.printStackTrace();
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		ByteBuffer sad = (ByteBuffer)obj;
		sad = (ByteBuffer)sad.rewind();
		System.out.println("Server onSent: "+MessageLoader.byteBufferToString(sad));
	}

}
