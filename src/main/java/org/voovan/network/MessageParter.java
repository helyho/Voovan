package org.voovan.network;


public interface MessageParter {

	/**
	 * 判断消息是否可区分
	 * @param buffer
	 * @param elapsedtime
	 * @return   返回:true 可区分,false不可区分
	 */
	public boolean canPartition(IoSession session,byte[] buffer,int elapsedtime);
	
}
