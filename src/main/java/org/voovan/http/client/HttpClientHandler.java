package org.voovan.http.client;

import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

/**
 * HTTP 请求处理句柄
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpClientHandler implements IoHandler {

	private Request request;
	private Response response;
	
	
	
	public HttpClientHandler(Request request){
		this.request = request;
		response = null;
	}
	
	public synchronized Response getResponse(){
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
		if(obj instanceof Response){
			response = TObject.cast(obj);
		}
		session.close();
		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		//不处理这个方法
	}

	@Override
	public void onException(IoSession session, Exception e) {
		session.close();
		Logger.error(e);
	}

}
