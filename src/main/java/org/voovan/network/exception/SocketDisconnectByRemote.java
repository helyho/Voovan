package org.voovan.network.exception;

import java.io.IOException;

/**
 * Socket 读取流管道被关闭,这个时候缓冲区可能还有数据没有读取完成,
 * 因为对方可能在发送完报文后主动关闭了连接.
 * 所以出现这个异常时极有可能还会返回有效的报文数据.
 */
public class SocketDisconnectByRemote extends IOException{
	private static final long	serialVersionUID	= 1L;
	
	public SocketDisconnectByRemote(String message){
		super(message);
	}
}
