package org.voovan.http.extend.engineio;

import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.exception.SendMessageException;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class EIOHandler {

    private WebSocketSession webSocketSession;

    protected void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public void send(String msg) throws SendMessageException, WebSocketFilterException {
        webSocketSession.send(EIOParser.encode(new EIOPacket(4, msg)));
    }

    public abstract String execute(String msg);
}

