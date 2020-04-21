package org.voovan.http.extend.engineio;

import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.collection.Attributes;

/**
 * Engine IO 会话类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EIOSession extends Attributes {

    private WebSocketSession webSocketSession;
    private EIOHandler eioHandler;

    public EIOSession(WebSocketSession webSocketSession, EIOHandler eioHandler){
        this.webSocketSession = webSocketSession;
        this.eioHandler = eioHandler;
    }

    protected void getEioHandler(EIOHandler eioHandler) {
        this.eioHandler = eioHandler;
    }

    protected WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public void send(String msg) throws SendMessageException, WebSocketFilterException {
        webSocketSession.send(EIOParser.encode(new EIOPacket(4, msg)));
    }
}
