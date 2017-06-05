package org.voovan.http.server.exception;

/**
 * 资源不存在异常
 * @author helyho
 *
 */
public class ResourceNotFound extends Exception {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;
	
	public ResourceNotFound(String description){
		super(description);
	}

}
