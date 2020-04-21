package org.voovan.http.extend.socketio;

import org.voovan.http.extend.engineio.EIOHandler;
import org.voovan.http.extend.engineio.EIOSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;

import java.util.ArrayList;
import java.util.List;

/**
 *  Socket IO 会话
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SIOSession {
    private SIODispatcher sioDispatcher;
    private EIOSession eioSession;
    private String nsp;

    public SIOSession(EIOSession eioSession, SIODispatcher sioDispatcher, String nsp){
        this.eioSession = eioSession;
        this.sioDispatcher = sioDispatcher;
        this.nsp = nsp;
    }

    protected void getEioSession(EIOHandler eioHandler) {
        this.eioSession = eioSession;
    }

    protected void getNsp(String nsp) {
        this.nsp = nsp;
    }

    protected void getSioDispatcher(SIODispatcher sioDispatcher) {
        this.sioDispatcher = sioDispatcher;
    }

    public void emit(String event, SIOHandler sioHandler, Object ... params) throws SendMessageException, WebSocketFilterException {
        sioDispatcher.on(event, sioHandler);

        List<Object> args = new ArrayList<Object>();
        args.add(event);
        args.addAll(TObject.asList(params));
        SIOPacket sioPacket = new SIOPacket(2, this.nsp, JSON.toJSON(args));
        eioSession.send(SIOParser.encode(sioPacket));
    }
}
