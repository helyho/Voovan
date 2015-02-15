package org.hocate.network;

public interface IoFilter {
	/**
	 * 过滤器解密函数,接收(onRecive)前调用
	 * 			onRecive事件前调用
	 * @param object
	 * @return
	 */
	public Object decode(IoSession session,Object object);
	
	/**
	 * 过滤器加密函数,发送(onSend)前调用
	 * 			send前调用
	 * @param object
	 * @return
	 */
	public Object encode(IoSession session,Object object);
	
}
