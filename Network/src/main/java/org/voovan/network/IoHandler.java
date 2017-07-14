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
	 * @param session Session 对象
	 * @return   返回的对象将发送
	 */
	public Object onConnect(IoSession session);
	
	/**
	 * 连接断开事件
	 * @param session Session 对象
	 */
	public void onDisconnect(IoSession session);
	
	/**
	 * 接收数据事件
	 * @param session Session 对象
	 * @param obj  接收的对象
	 * @return 返回的对象将发送
	 */
	public Object onReceive(IoSession session, Object obj);

	/**
	 * 发送数据事件
	 * 		发送后调用
	 * @param session Session 对象
	 * @param obj 发送的对象
	 */
	public void onSent(IoSession session, Object obj);

	/**
	 * 异常事件
	 * @param session Session 对象
	 * @param e 异常信息
	 */
	public void onException(IoSession session, Exception e);

	/**
	 * 空闲事件
	 * @param session Session 对象
	 */
	public void onIdle(IoSession session);
}
