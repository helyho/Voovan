package org.voovan.network.exception;

import java.io.IOException;

public class IoFilterException extends IOException {

	private static final long	serialVersionUID	= 1L;

	public IoFilterException(String message,Exception e){
		super(message);
		this.setStackTrace(e.getStackTrace());
	}
	
	public IoFilterException(String message){
		super(message);
	}
}
