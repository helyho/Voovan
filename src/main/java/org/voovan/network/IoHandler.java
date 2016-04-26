package org.voovan.network;


/**
 * Socket 连接业务接口
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface IoHandler {
	/**
	 * 连接成功事件
	 * @param session
	 * @return   返回的对象将发送
	 */
	public Object onConnect(IoSession session);
	
	/**
	 * 连接断开事件
	 * @param session
	 */
	public void onDisconnect(IoSession session);
	
	/**
	 * 接收数据事件
	 * @param session
	 * @param obj 
	 * @return 返回的对象将发送
	 */
	public Object onReceive(IoSession session,Object obj);
	
	/**
	 * 发送数据事件
	 * 		发送后调用
	 * @param session
	 * @param obj 
	 */
	public void onSent(IoSession session,Object obj);	
	
	/**
	 * 异常事件
	 * @param session
	 * @param e
	 */
	public void onException(IoSession session,Exception e);
}
