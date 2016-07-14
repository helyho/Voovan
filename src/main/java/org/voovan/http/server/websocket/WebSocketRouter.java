package org.voovan.http.server.websocket;

import org.voovan.http.server.HttpRequest;

import java.nio.ByteBuffer;

/**
 * WebSocket 处理句柄
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface WebSocketRouter {
	public void onOpen(HttpRequest upgradeRequest);
	public ByteBuffer onRecived(HttpRequest upgradeRequest,ByteBuffer message);
	public void onClose();
}
