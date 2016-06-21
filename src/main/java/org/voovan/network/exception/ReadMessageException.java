package org.voovan.network.exception;

public class ReadMessageException extends Exception {

	private static final long	serialVersionUID	= 1L;

	public ReadMessageException(String message, Exception e){
		super(message);
		this.setStackTrace(e.getStackTrace());
	}

	public ReadMessageException(String message){
		super(message);
	}

	public ReadMessageException(Exception e){
		this.setStackTrace(e.getStackTrace());
	}
}
