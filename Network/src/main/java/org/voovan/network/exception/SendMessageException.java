package org.voovan.network.exception;

import java.io.IOException;

public class SendMessageException extends IOException {

	private static final long	serialVersionUID	= 1L;

	public SendMessageException(String message,Exception e){
		super(message);
		this.setStackTrace(e.getStackTrace());
	}
	
	public SendMessageException(String message){
		super(message);
	}

	public SendMessageException(Exception e){
		this.setStackTrace(e.getStackTrace());
	}
}
