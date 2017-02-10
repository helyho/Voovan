package org.voovan.http.websocket;

import org.voovan.http.server.HttpRequest;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;

import java.io.IOException;
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
		if(this.session==null) {
			this.session = session;
		}
	}

	/**
	 * 关闭 WebSocket
	 * @throws IOException Io 异常
	 */
	public void close() {
		try {
            WebSocketFrame closeWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING,
                    false, ByteBuffer.wrap(WebSocketTools.intToByteArray(1000, 2)));
            session.setAttribute("WebSocketClose", true);
            session.synchronouSend(closeWebSocketFrame.toByteBuffer());
			session.close();
		} catch (SendMessageException e) {
			e.printStackTrace();
		}
	}

	/**
	 * websocket 连接打开
	 * @return
	 */
	public abstract ByteBuffer onOpen();

	/**
	 * websocket 收到消息
	 * @param message
	 * @return
	 */
	public abstract ByteBuffer onRecived(ByteBuffer message);

	/**
	 * websocket 消息发送完成
	 * @param message
	 * @return
	 */
	public abstract void onSent(ByteBuffer message);


	/**
	 * websocket 关闭
	 */
	public abstract void onClose();
}
