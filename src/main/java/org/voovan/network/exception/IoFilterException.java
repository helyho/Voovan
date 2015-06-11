package org.voovan.network.exception;

public class IoFilterException extends Exception {

	private static final long	serialVersionUID	= 1L;

	public IoFilterException(String message,Exception e){
		super(message);
		this.setStackTrace(e.getStackTrace());
	}
	
	public IoFilterException(String message){
		super(message);
	}
}
