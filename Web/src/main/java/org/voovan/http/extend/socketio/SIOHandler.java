package org.voovan.http.extend.socketio;

import org.voovan.http.extend.engineio.EIOHandler;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    protected void setEioHandler(EIOHandler eioHandler) {
        this.eioHandler = eioHandler;
    }

    protected void setNsp(String nsp) {
        this.nsp = nsp;
    }

    protected void setSioDispatcher(SIODispatcher sioDispatcher) {
        this.sioDispatcher = sioDispatcher;
    }

    public void emit(String event, SIOHandler sioHandler, Object ... params) throws SendMessageException, WebSocketFilterException {
        sioDispatcher.on(event, sioHandler);

        List<Object> args = new ArrayList<Object>();
        args.add(event);
        args.addAll(TObject.asList(params));
        SIOPacket sioPacket = new SIOPacket(2, this.nsp, JSON.toJSON(args));
        eioHandler.send(SIOParser.encode(sioPacket));
    }

    public abstract Object execute(Object ... args);
}
