package org.voovan.http.server.websocket;

import java.nio.ByteBuffer;

import org.voovan.http.server.HttpRequest;

/**
 * WebSocket 处理句柄
 * @author helyho
 *
 */
public interface WebSocketBizHandler {
	public void onOpen(HttpRequest upgradeRequest);
	public ByteBuffer onRecived(HttpRequest upgradeRequest,ByteBuffer message);
	public void onClose();
}
