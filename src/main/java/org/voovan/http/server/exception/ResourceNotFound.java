package org.voovan.http.server.exception;

public class ResourceNotFound extends Exception {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	
	public ResourceNotFound(String description){
		super(description);
	}

}
