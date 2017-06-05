package org.voovan.network;

import java.nio.ByteBuffer;

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
	 * @param byteBuffer 缓冲数据
	 * @return   返回: 大于0或者等于0可区分,小于不0可区分
	 * 					返回的int数据值,则被用于从缓冲区取值给onRecive函数作为参数的的数据的长度.
	 */
	public int canSplite(IoSession session, ByteBuffer byteBuffer);
	
}
