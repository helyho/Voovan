package org.voovan.http.server.exception;

/**
 * 路由不存在异常
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RouterNotFound extends Exception {

	private static final long	serialVersionUID	= 1L;
	
	public RouterNotFound(String description){
		super(description);
	}

}
