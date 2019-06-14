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
	 * 		分割处理: 这个时候返回长度需要大于 0, 这个时候 session.getByteBuffer 中的数据会被分割, 然后传递给 IoFilter
	 * 		流式处理: 用户 http/1.1 等协议解析, 这个时候返回长度需要等于0, 则数据不会被分割, 所有数据都在 session.getByteBuffer 中, 这个时候数据可以在 IoFilter 中进行处理,
	 * 				  这个时候 IoFilter 接收到的是一个容量为 0 的 bytebuffer
	 * @param session  session 对象
	 * @param byteBuffer 缓冲数据
	 * @return   返回: 大于0或者等于0可区分,小于不0可区分,则继续接收数据
	 * 				   返回的int数据值,则被用于从缓冲区取值给onRecive函数作为参数的的数据的长度.
	 *
	 */
	public int canSplite(IoSession session, ByteBuffer byteBuffer);

}
