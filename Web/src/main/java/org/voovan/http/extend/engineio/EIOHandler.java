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

    public void setWebSocketSession(WebSocketSession webSocketSession) {
         this.webSocketSession = webSocketSession;
    }


    public EIOSession getEIOSession() {
        EIOSession eioSession = (EIOSession) webSocketSession.getAttribute("EIO_SESSION");
        if(eioSession == null) {
            eioSession = new EIOSession(this.webSocketSession, this);
            webSocketSession.setAttribute("EIOSession", eioSession);
        }
        return eioSession;
    }

    public void send(String msg) throws SendMessageException, WebSocketFilterException {
        this.getEIOSession().send(msg);
    }

    public abstract String execute(String msg);
}

