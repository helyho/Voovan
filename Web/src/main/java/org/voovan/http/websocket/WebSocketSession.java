package org.voovan.http.websocket;

import org.voovan.http.server.*;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.collection.Attributes;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

/**
 * WebSocket 会话对象
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebSocketSession extends Attributes {
    private IoSession socketSession;
    private WebSocketRouter webSocketRouter;
    private String remoteAddres;
    private int remotePort;
    private WebSocketType webSocketType;
    private boolean masked;


    /**
     * 构造函数
     * @param socketSession Socket 会话
     * @param webSocketRouter WebSocket 路由处理对象
     * @param webSocketType WebSocket类型
     */
    public WebSocketSession(IoSession socketSession, WebSocketRouter webSocketRouter, WebSocketType webSocketType){
        this.socketSession = socketSession;
        this.remoteAddres = socketSession.remoteAddress();
        this.remotePort = socketSession.remotePort();
        this.webSocketRouter = webSocketRouter;
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
        HttpSessionState httpSessionState = WebServerHandler.getAttachment(socketSession);
        HttpRequest request = httpSessionState.getHttpRequest();
        return request.protocol().getPath();
    }

    /**
     * 获取 Http 的 session
     * @return HttpSession对象
     */
    public HttpSession getHttpSession(){
        HttpSessionState httpSessionState = WebServerHandler.getAttachment(socketSession);
        HttpRequest request = httpSessionState.getHttpRequest();
        if(request.sessionExists()) {
            return request.getSession();
        } else {
            return null;
        }
    }

    /**
     * 获取对端连接的 IP
     *
     * @return 对端连接的 IP
     */
    public String getRemoteAddress() {
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

        ByteBuffer byteBuffer = (ByteBuffer) WebSocketDispatcher.filterEncoder(this, obj);
        WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, masked, byteBuffer);
        this.socketSession.syncSend(webSocketFrame);
    }

    /**
     * 发送 websocket 消息二进制消息
     * @param obj 消息对象
     * @throws SendMessageException 发送异常
     * @throws WebSocketFilterException WebSocket过滤器异常
     */
    public void sendBinary(Object obj) throws SendMessageException, WebSocketFilterException {
        ByteBuffer byteBuffer = null;
        if(obj instanceof byte[]){
            byteBuffer = ByteBuffer.wrap((byte[])obj);
        } else if(obj instanceof ByteBuffer) {
            byteBuffer = (ByteBuffer)obj;
        } else {
            byteBuffer = (ByteBuffer)WebSocketDispatcher.filterEncoder(this, obj);
        }

        WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.BINARY, masked, byteBuffer);
        this.socketSession.syncSend(webSocketFrame);
    }

    /**
     * 发送 websocket 帧
     * @param webSocketFrame 帧
     * @throws SendMessageException 发送异常
     */
    protected void send(WebSocketFrame webSocketFrame) throws SendMessageException {
        this.socketSession.syncSend(webSocketFrame);
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
