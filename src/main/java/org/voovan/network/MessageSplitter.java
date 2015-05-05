package org.voovan.network;

/**
 * 消息分割类
 * 
 * @author helyho
 *
 */
public interface MessageSplitter {

	/**
	 * 判断消息是否可分割
	 * @param buffer
	 * @param elapsedtime
	 * @return   返回:true 可区分,false不可区分
	 */
	public boolean canSplite(IoSession session,byte[] buffer,int elapsedtime);
	
}
