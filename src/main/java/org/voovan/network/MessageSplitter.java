package org.voovan.network;

/**
 * 消息分割类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface MessageSplitter {

	/**
	 * 判断消息是否可分割
	 * @param session  session 对象
	 * @param buffer 缓冲数据
	 * @return   返回:true 可区分,false不可区分
	 */
	public boolean canSplite(IoSession session,byte[] buffer);
	
}
