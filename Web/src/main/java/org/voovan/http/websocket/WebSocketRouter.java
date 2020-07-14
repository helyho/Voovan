package org.voovan.http.websocket;

import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.tools.collection.Chain;

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

	protected Chain<WebSocketFilter> webSocketFilterChain;

	public WebSocketRouter(){
		webSocketFilterChain = new Chain<WebSocketFilter>();
	}

	public WebSocketRouter addFilterChain(WebSocketFilter webSocketFilter) {
		webSocketFilterChain.add(webSocketFilter);
		return this;
	}

	public WebSocketRouter clearFilterChain() {
		webSocketFilterChain.clear();
		return this;
	}

	public WebSocketRouter removeFilterChain(WebSocketFilter webSocketFilter) {
		webSocketFilterChain.remove(webSocketFilter);
		return this;
	}

	public Chain<WebSocketFilter> getWebSocketFilterChain() {
		return webSocketFilterChain;
	}

	/**
	 * websocket 连接打开
	 * @param session WebSocket 会话
	 * @return 收到的缓冲数据
	 */
	public abstract Object onOpen(WebSocketSession session);

	/**
	 * websocket 收到消息
	 * @param session WebSocket 会话
	 * @param obj 收到的缓冲数据
	 * @return 收到的缓冲数据
	 */
	public abstract Object onRecived(WebSocketSession session, Object obj);

	/**
	 * websocket 消息发送完成
	 * @param session WebSocket 会话
	 * @param obj 发送的消息
	 */
	public abstract void onSent(WebSocketSession session, Object obj);


	/**
	 * websocket 关闭
	 * @param session WebSocket 会话
	 */
	public abstract void onClose(WebSocketSession session);
}
