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
	 * @return   返回: 大于0可区分,小于或者等于0不可区分
	 * 					返回的int数据值,则被用于从缓冲区取值给onRecive函数作为参数的的数据的长度.
	 */
	public int canSplite(IoSession session,byte[] buffer);
	
}
