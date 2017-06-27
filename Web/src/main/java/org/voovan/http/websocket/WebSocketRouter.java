package org.voovan.http.websocket;

import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;

import java.nio.ByteBuffer;

/**
 * WebSocket 处理句柄
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class WebSocketRouter {
	private IoSession session;

	/**
	 * 设置会话
	 * @param session IoSession 会话对象
	 */
	public void setSession(IoSession session){
		this.session = session;
	}

	/**
	 * 发送消息给客户端
	 * @param byteBuffer ByteBuffer 对象
	 * @throws SendMessageException 发送异常
	 */
	public synchronized void send(ByteBuffer byteBuffer) {
		WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, false, byteBuffer);
		session.send(webSocketFrame.toByteBuffer());
		byteBuffer.remaining();

		//出发发送事件
		onSent(byteBuffer);
	}

	/**
	 * 关闭 WebSocket
	 */
	public void close() {
		try {
            WebSocketFrame closeWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING,
                    false, ByteBuffer.wrap(WebSocketTools.intToByteArray(1000, 2)));
            session.setAttribute("WebSocketClose", true);
            session.syncSend(closeWebSocketFrame);
		} catch (SendMessageException e) {
			e.printStackTrace();
		}
	}

	/**
	 * websocket 连接打开
	 * @return 收到的缓冲数据
	 */
	public abstract ByteBuffer onOpen();

	/**
	 * websocket 收到消息
	 * @param message 收到的缓冲数据
	 * @return 收到的缓冲数据
	 */
	public abstract ByteBuffer onRecived(ByteBuffer message);

	/**
	 * websocket 消息发送完成
	 * @param message 发送的消息
	 */
	public abstract void onSent(ByteBuffer message);


	/**
	 * websocket 关闭
	 */
	public abstract void onClose();
}
