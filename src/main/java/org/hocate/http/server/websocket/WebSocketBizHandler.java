package org.hocate.http.server.websocket;

import java.nio.ByteBuffer;
import java.util.Map;

public interface WebSocketBizHandler {
	public void onOpen(Map<String, String> params);
	public ByteBuffer onRecived(Map<String, String> params,ByteBuffer message);
	public void onClose();
}
