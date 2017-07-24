package org.voovan.http.extend.socketio;

import org.voovan.http.extend.engineio.EIOHandler;
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
public abstract class SIOHandler {
    private EIOHandler eioHandler;
    private SIODispatcher sioDispatcher;
    private String nsp;
    private SIOSession sioSession;

    protected void setEioHandler(EIOHandler eioHandler) {
        this.eioHandler = eioHandler;
    }

    protected void setNsp(String nsp) {
        this.nsp = nsp;
    }

    protected void setSioDispatcher(SIODispatcher sioDispatcher) {
        this.sioDispatcher = sioDispatcher;
    }

    public SIOSession getSIOSession(){
        if(sioSession == null){
            sioSession = new SIOSession(eioHandler.getEIOSession(), sioDispatcher, nsp);
        }

        return sioSession;
    }

    public void emit(String event, SIOHandler sioHandler, Object ... params) throws SendMessageException, WebSocketFilterException {
        this.getSIOSession().emit(event, sioHandler, params);
    }

    public abstract Object execute(Object ... args);
}
