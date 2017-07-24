package org.voovan.http.extend.socketio;

import org.voovan.http.extend.engineio.EIOHandler;
import org.voovan.http.extend.engineio.EIOSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SIOSession {
    private SIODispatcher sioDispatcher;
    private Map<String,Object> attributes;
    private EIOSession eioSession;
    private String nsp;

    public SIOSession(EIOSession eioSession, SIODispatcher sioDispatcher, String nsp){
        attributes = new ConcurrentHashMap<String, Object>();
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

    /**
     * 获取当前 Session 属性
     * @param name 属性名
     * @return 属性值
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * 判断当前 Session 属性是否存在
     * @param name 属性名
     * @return true: 存在, false: 不存在
     */
    public boolean containAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * 设置当前 Session 属性
     * @param name	属性名
     * @param value	属性值
     */
    public void setAttribute(String name,Object value) {
        attributes.put(name, value);
    }

    /**
     *  删除当前 Session 属性
     * @param name	属性名
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
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
