package org.voovan.network.exception;

import java.io.IOException;

public class SocketDisconnectByRemote extends IOException{
	private static final long	serialVersionUID	= 1L;
	
	public SocketDisconnectByRemote(String message){
		super(message);
	}
}
