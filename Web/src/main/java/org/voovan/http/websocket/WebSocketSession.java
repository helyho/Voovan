package org.voovan.http.websocket;

import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpSession;
import org.voovan.http.server.WebServerHandler;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话对象
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebSocketSession {
    private IoSession socketSession;
    private WebSocketRouter webSocketRouter;
    private String remoteAddres;
    private int remotePort;
    private WebSocketType webSocketType;
    private boolean masked;

    private Map<String,Object> attributes;

    /**
     * 构造函数
     * @param socketSession Socket 会话
     * @param webSocketRouter WebSocket 路由处理对象
     */
    public WebSocketSession(IoSession socketSession, WebSocketRouter webSocketRouter, WebSocketType webSocketType){
        this.socketSession = socketSession;
        this.remoteAddres = socketSession.remoteAddress();
        this.remotePort = socketSession.remotePort();
        this.webSocketRouter = webSocketRouter;
        attributes = new ConcurrentHashMap<String, Object>();
        this.webSocketType = webSocketType;
        if(this.webSocketType == webSocketType.SERVER){
            masked = false;
        } else {
            masked = true;
        }
    }

    /**
     * 获取WebSocket的地址
     * @return WebSocket的地址
     */
    public String getLocation(){
        HttpRequest request = (HttpRequest)socketSession.getAttribute(WebServerHandler.SessionParam.HTTP_REQUEST);
        return request.protocol().getPath();
    }

    /**
     * 获取 Http 的 session
     * @return HttpSession对象
     */
    public HttpSession getHttpSession(){
        HttpRequest request = (HttpRequest)socketSession.getAttribute(WebServerHandler.SessionParam.HTTP_REQUEST);
        return request.getSession();
    }

    /**
     * 获取对端连接的 IP
     *
     * @return 对端连接的 IP
     */
    public String getRemoteAddres() {
        return this.remoteAddres;
    }

    /**
     * 获取对端连接的端口
     *
     * @return 对端连接的端口
     */
    public int getRemotePort() {
        return remotePort;
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

    /**
     * 获取 WebSocket 路由处理对象
     * @return WebSocket 路由处理对象
     */
    public WebSocketRouter getWebSocketRouter() {
        return webSocketRouter;
    }

    /**
     * 设置获取WebSocket 路由处理对象
     * @param webSocketRouter WebSocket 路由处理对象
     */
    public void setWebSocketRouter(WebSocketRouter webSocketRouter) {
        this.webSocketRouter = webSocketRouter;
    }

    /**
     * 发送 websocket 消息
     * @param obj 消息对象
     * @throws SendMessageException 发送异常
     * @throws WebSocketFilterException WebSocket过滤器异常
     */
    public void send(Object obj) throws SendMessageException, WebSocketFilterException {

        ByteBuffer byteBuffer = (ByteBuffer)webSocketRouter.filterEncoder(this, obj);
        WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, masked, byteBuffer);
        this.socketSession.syncSend(webSocketFrame);
    }

    /**
     * 发送 websocket 帧
     * @param webSocketFrame 帧
     * @throws SendMessageException 发送异常
     */
    protected void send(WebSocketFrame webSocketFrame) throws SendMessageException {
        this.socketSession.syncSend(webSocketFrame);

        if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.TEXT ||
                webSocketFrame.getOpcode() == WebSocketFrame.Opcode.BINARY) {
            //触发发送事件
            webSocketRouter.onSent(this, webSocketFrame.getFrameData());
        }
    }

    /**
     * 判断连接状态
     * @return true: 连接状态, false: 断开状态
     */
    public boolean isConnected(){
        return socketSession.isConnected();
    }

    /**
     * 直接关闭 Socket 连接
     *      不会发送 CLOSING 给客户端
     */
    /**
     * 关闭 WebSocket
     */
    public void close() {
        WebSocketFrame closeWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING,
                masked, ByteBuffer.wrap(WebSocketTools.intToByteArray(1000, 2)));
        try {
            send(closeWebSocketFrame);
        } catch (SendMessageException e) {
            Logger.error("Close WebSocket error, Socket will be close " ,e);
            socketSession.close();
        }
    }

    protected IoSession getSocketSession() {
        return socketSession;
    }

    public void setSocketSession(IoSession socketSession) {
        this.socketSession = socketSession;
    }

}
