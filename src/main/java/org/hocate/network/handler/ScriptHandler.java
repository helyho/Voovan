package org.hocate.network.handler;

import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.script.ScriptObject;

public class ScriptHandler implements IoHandler {
	private ScriptObject scriptObject;

	public ScriptHandler(ScriptObject scriptObject){
		this.scriptObject = scriptObject;
	}
	
	@Override
	public Object onConnect(IoSession session) {
		return scriptObject.callMethod("onConnect",session);
	}

	@Override
	public void onDisconnect(IoSession session) {
		scriptObject.callMethod("onDisconnect",session);

	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		return scriptObject.callMethod("onRecive",session,obj);
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		scriptObject.callMethod("onSent",session,obj);
	}

	@Override
	public void onException(IoSession session, Exception e) {
		scriptObject.callMethod("onException",session,e.getMessage());
	}

}
