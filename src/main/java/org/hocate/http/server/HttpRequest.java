package org.hocate.http.server;

import org.hocate.http.message.Request;
import org.hocate.http.message.packet.Cookie;

public class HttpRequest extends Request {

	private HttpSession session;
	
	protected HttpRequest(Request request){
		super(request);
	}

	public Cookie getCookie(String name){
		for(Cookie cookie : this.cookies()){
			if(cookie.getName().equals(name)){
				return cookie;
			}
		}
		return null;
	}

	public HttpSession getSession() {
		return session;
	}

	protected void setSession(HttpSession session) {
		this.session = session;
	}
}
