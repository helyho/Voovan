package org.voovan.http.client;

import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.WebSocketTools;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;

import java.nio.ByteBuffer;

/**
 * 处理 WebSocket 相关的 IoHandler 事件
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class WebSocketHandler implements IoHandler{

    private WebSocketRouter webSocketRouter;
    private HttpClient httpClient;
    private WebSocketSession webSocketSession;

    /**
     * 构造函数
     * @param httpClient  HttpClient对象
     * @param webSocketSession WebSocketSession对象
     * @param webSocketRouter WebSocketRouter对象
     */
    public WebSocketHandler(HttpClient httpClient, WebSocketSession webSocketSession, WebSocketRouter webSocketRouter){
        this.webSocketRouter = webSocketRouter;
        this.httpClient = httpClient;
        this.webSocketSession = webSocketSession;
    }

    @Override
    public Object onConnect(IoSession session) {
        //不会被触发
        return null;
    }

    @Override
    public void onDisconnect(IoSession session) {
        //触发 onClose
        webSocketRouter.onClose(webSocketSession);

        //WebSocket 要考虑释放缓冲区
        ByteBufferChannel byteBufferChannel = TObject.cast(session.getAttribute("WebSocketByteBufferChannel"));
        if (byteBufferChannel != null && !byteBufferChannel.isReleased()) {
            byteBufferChannel.release();
        }
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {

        //分片 (1) fin=0 , opcode=1
        //分片 (2) fin=0 , opcode=0
        //分片 (3) fin=1 , opcode=0

        WebSocketFrame respWebSocketFrame = null;
        WebSocketFrame reqWebSocketFrame = null;
        if(obj instanceof WebSocketFrame) {
            reqWebSocketFrame = TObject.cast(obj);
        }else{
            return null;
        }

        ByteBufferChannel byteBufferChannel = null;
        if(!session.containAttribute("WebSocketByteBufferChannel")){
            byteBufferChannel = new ByteBufferChannel(session.socketContext().getBufferSize());
            session.setAttribute("WebSocketByteBufferChannel",byteBufferChannel);
        }else{
            byteBufferChannel = TObject.cast(session.getAttribute("WebSocketByteBufferChannel"));
        }


        // WS_CLOSE 如果收到关闭帧则关闭连接
        if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.CLOSING) {
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, reqWebSocketFrame.getFrameData());
        }
        // WS_PING 收到 ping 帧则返回 pong 帧
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.PING) {
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PONG, false,  reqWebSocketFrame.getFrameData());
        }
        // WS_PING 收到 pong 帧则返回 ping 帧
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.PONG) {
            TEnv.sleep(1000);
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, false, null);
        }else if(reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.CONTINUOUS){
            byteBufferChannel.writeEnd(reqWebSocketFrame.getFrameData());
        }
        // WS_RECIVE 文本和二进制消息触发 Recived 事件
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.TEXT || reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.BINARY) {
            Object result = null;

            byteBufferChannel.writeEnd(reqWebSocketFrame.getFrameData());

            //解包
            result = webSocketRouter.filterDecoder(webSocketSession, byteBufferChannel.getByteBuffer());
            byteBufferChannel.compact();

            //触发 onRecive
            result = webSocketRouter.onRecived(webSocketSession, result);

            //封包
            ByteBuffer buffer = (ByteBuffer) webSocketRouter.filterEncoder(webSocketSession, result);

            //判断解包是否有错
            if (reqWebSocketFrame.getErrorCode() == 0) {
                respWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, true, buffer);
            } else {
                //解析时出现异常,返回关闭消息
                respWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, ByteBuffer.wrap(WebSocketTools.intToByteArray(reqWebSocketFrame.getErrorCode(), 2)));
            }
        }

        return respWebSocketFrame;
    }

    @Override
    public void onSent(IoSession session, Object obj) {
        WebSocketFrame webSocketFrame = (WebSocketFrame)obj;
        if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.CLOSING){
            session.close();
            return;
        }

        if(webSocketFrame.getOpcode() != WebSocketFrame.Opcode.PING &&
                webSocketFrame.getOpcode() != WebSocketFrame.Opcode.PONG &&
                webSocketFrame.getOpcode() != WebSocketFrame.Opcode.CLOSING) {
            ByteBuffer data = webSocketFrame.getFrameData();

            //解包
            obj = webSocketRouter.filterDecoder(webSocketSession, data);

            //触发 onSent
            webSocketRouter.onSent(webSocketSession, obj);
        }
    }

    @Override
    public void onException(IoSession session, Exception e) {

    }
}
