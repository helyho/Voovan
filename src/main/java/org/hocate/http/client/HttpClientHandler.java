package org.hocate.http.client;

import org.hocate.http.message.HttpRequest;
import org.hocate.http.message.HttpResponse;
import org.hocate.log.Logger;
import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.tools.TObject;

public class HttpClientHandler implements IoHandler {

	private HttpRequest request;
	private HttpResponse response;
	
	
	
	public HttpClientHandler(HttpRequest request){
		this.request = request;
		response = null;
	}
	
	public synchronized HttpResponse getResponse(){
		return response;
	}
	
	@Override
	public Object onConnect(IoSession session) {
		return request;
	}

	@Override
	public void onDisconnect(IoSession session) {
		Logger.debug("Socket disconnect!");
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		if(obj instanceof HttpResponse){
			response = TObject.cast(obj);
		}
		session.close();
		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {

	}

	@Override
	public void onException(IoSession session, Exception e) {
		session.close();
		e.printStackTrace();
	}

}
