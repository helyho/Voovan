package org.voovan.http.websocket;

import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

/**
 * WebSocket 处理句柄
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class WebSocketRouter implements Cloneable{
	private WebSocketSession session;

	/**
	 * 设置会话
	 * @param session IoSession 会话对象
	 */
	public void setSession(WebSocketSession session){
		this.session = session;
	}

	/**
	 * websocket 连接打开
	 * @return 收到的缓冲数据
	 */
	public abstract ByteBuffer onOpen(WebSocketSession session);

	/**
	 * websocket 收到消息
	 * @param message 收到的缓冲数据
	 * @return 收到的缓冲数据
	 */
	public abstract ByteBuffer onRecived(WebSocketSession session, ByteBuffer message);

	/**
	 * websocket 消息发送完成
	 * @param message 发送的消息
	 */
	public abstract void onSent(WebSocketSession session, ByteBuffer message);


	/**
	 * websocket 关闭
	 */
	public abstract void onClose(WebSocketSession session);
}
