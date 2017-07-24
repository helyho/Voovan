package org.voovan.http.extend.engineio;

import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.exception.SendMessageException;

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
public class EIOSession {

    private Map<String,Object> attributes;
    private WebSocketSession webSocketSession;
    private EIOHandler eioHandler;

    public EIOSession(WebSocketSession webSocketSession, EIOHandler eioHandler){
        attributes = new ConcurrentHashMap<String, Object>();
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

    public void send(String msg) throws SendMessageException, WebSocketFilterException {
        webSocketSession.send(EIOParser.encode(new EIOPacket(4, msg)));
    }
}
