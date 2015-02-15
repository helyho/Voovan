package org.hocate.http.server.websocket;

import java.nio.ByteBuffer;

public interface WebSocketHandler {
	public ByteBuffer onOpen();
	public ByteBuffer onRecived();
	public ByteBuffer onSent();
	public ByteBuffer onClose();
}
