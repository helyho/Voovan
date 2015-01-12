package org.hocate.test.network;

import java.nio.ByteBuffer;

import org.hocate.log.Logger;
import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.network.MessageLoader;
import org.hocate.tools.TFile;

public class ClientHandlerTest implements IoHandler {

	@Override
	public Object onConnect(IoSession session) {
		Logger.simple("onConnect");
		session.setAttribute("key", "attribute value");
		String msg = new String(TFile.loadFileFromSysPath("/Users/helyho/1.txt"));
		return msg;//"Client Message";
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
		e.printStackTrace();
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		ByteBuffer sad = (ByteBuffer)obj;
		sad = (ByteBuffer)sad.rewind();
		Logger.simple("Client onSent: "+MessageLoader.byteBufferToString(sad));
	}

}
