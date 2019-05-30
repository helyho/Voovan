package org.voovan.network;

import org.voovan.network.exception.IoFilterException;

/**
 * 过滤器接口
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface IoFilter {
	/**
	 * 过滤器解密函数,接收事件(onRecive)前调用
	 * 			如果任何一个过滤器返回的是null,  则它后面的过滤器不会被执行, 且不会执行 onRevcive
	 * 	     	如果最后一个过滤器如果返回 null, 则不会进行路由处理
	 * @param session  session 对象
	 * @param object   解码对象,上一个过滤器的返回值
	 * @return 解码后对象
	 * @throws IoFilterException 过滤器异常
	 */
	public Object decode(IoSession session, Object object) throws IoFilterException;

	/**
	 * 过滤器加密函数,发送事件(onSend)前后用
	 * 			send事件后调用
	 * @param session 	session 对象
	 * @param object    编码对象,上一个过滤器的返回值
	 * @return 编码后对象
	 * 			最后一个过滤器返回的数据只支持三种数据类型: ByteBuffer, String, byte[]
	 * @throws IoFilterException 过滤器异常
	 */
	public Object encode(IoSession session, Object object)throws IoFilterException;
	
}
