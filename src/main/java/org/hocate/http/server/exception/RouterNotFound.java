package org.hocate.http.server.exception;

public class RouterNotFound extends Exception {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	
	public RouterNotFound(String description){
		super(description);
	}

}
