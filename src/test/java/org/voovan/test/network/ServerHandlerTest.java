package org.voovan.test.network;


import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SocketDisconnectByRemote;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

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
		return "===="+obj.toString().trim()+" ===== \r\n";
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
		ByteBuffer sad = (ByteBuffer)obj;
		sad = (ByteBuffer)sad.rewind();
		Logger.simple("Server onSent: "+new String(sad.array()));
		//jmeter 测试是需要打开,和客户端测试时关闭
		//session.close();
	}

}
