package org.voovan.test.network;

import org.voovan.network.ConnectModel;
import org.voovan.network.HeartBeat;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

public class ClientHandlerTest implements IoHandler {

	@Override
	public Object onConnect(IoSession session) {
		System.out.println("onConnect");
		System.out.println("Connect from: "+session.remoteAddress()+":"+session.remotePort()+" "+session.loaclPort());
		session.setAttribute("key", "attribute value");
		String msg = new String("test message\r\n");
		return msg;
	}

	@Override
	public void onDisconnect(IoSession session) {
		System.out.println("onDisconnect");
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		System.out.println("Recive from: "+session.remoteAddress()+":"+session.remotePort()+" "+session.loaclPort());
		//+"["+session.remoteAddress()+":"+session.remotePort()+"]"
		System.out.println("Client onRecive: "+obj.toString());
		System.out.println("Attribute onRecive: "+session.getAttribute("key"));
		TEnv.sleep(3000);
		session.close();
		return null;
	}

	@Override
	public void onException(IoSession session, Exception e) {
		System.out.println("Client Exception: "+ e.getClass() + " => " +e.getMessage());
		Logger.error(e);
		session.close();
	}

	@Override
	public void onIdle(IoSession session) {
		//心跳依赖于 idle 时间,这个参数在构造 socket 的时候设置具体查看 org.voovan.network.aio.AioSocket

		//心跳绑定到 Session
		HeartBeat.attachSession(session, ConnectModel.SERVER, "PINGq", "PONGq");

		//心跳一次, 返回 true:本次心跳成功, false: 本次心跳失败
		System.out.println("==>"+HeartBeat.beat(session));
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		String sad = (String)obj;
		System.out.println("Client onSent: "+ sad);
	}

}
