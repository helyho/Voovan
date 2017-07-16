package org.voovan.http.websocket.exception;

import java.io.IOException;

public class WebSocketFilterException extends IOException {

	private static final long	serialVersionUID	= 1L;

	public WebSocketFilterException(String message, Exception e){
		super(message);
		this.setStackTrace(e.getStackTrace());
	}

	public WebSocketFilterException(String message){
		super(message);
	}
}
