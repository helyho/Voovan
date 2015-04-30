package org.voovan.http.server.websocket;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * WebSocket 处理句柄
 * @author helyho
 *
 */
public interface WebSocketBizHandler {
	public void onOpen(Map<String, String> params);
	public ByteBuffer onRecived(Map<String, String> params,ByteBuffer message);
	public void onClose();
}
