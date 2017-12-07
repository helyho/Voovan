package org.voovan.http.client;

import org.voovan.Global;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

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
        ByteBufferChannel byteBufferChannel = (ByteBufferChannel)session.getAttribute("WebSocketByteBufferChannel");
        if (byteBufferChannel != null && !byteBufferChannel.isReleased()) {
            byteBufferChannel.release();
        }
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {

        //分片 (1) fin=0 , opcode=1
        //分片 (2) fin=0 , opcode=0
        //分片 (3) fin=1 , opcode=0

        WebSocketFrame reqWebSocketFrame = null;
        WebSocketFrame respWebSocketFrame = null;
        if(obj instanceof WebSocketFrame) {
            reqWebSocketFrame = (WebSocketFrame)obj;
        }else{
            return null;
        }

        ByteBufferChannel byteBufferChannel = null;
        if(!session.containAttribute("WebSocketByteBufferChannel")){
            byteBufferChannel = new ByteBufferChannel(session.socketContext().getBufferSize());
            session.setAttribute("WebSocketByteBufferChannel",byteBufferChannel);
        }else{
            byteBufferChannel = (ByteBufferChannel)session.getAttribute("WebSocketByteBufferChannel");
        }


        // WS_CLOSE 如果收到关闭帧则关闭连接
        if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.CLOSING) {
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, true, reqWebSocketFrame.getFrameData());
        }
        // WS_PING 收到 ping 帧则返回 pong 帧
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.PING) {
            return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PONG, true,  reqWebSocketFrame.getFrameData());
        }
        // WS_PONG 收到 pong 帧则返回 ping 帧
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.PONG) {
            final IoSession poneSession = session;

            Global.getHashWheelTimer().addTask(new HashWheelTask() {
                @Override
                public void run() {
                    try {
                        Logger.simple("PONG");
                        poneSession.syncSend(WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, true, null));
                    } catch (SendMessageException e) {
                        poneSession.close();
                        Logger.error("WebSocket Pong event send Ping frame error", e);
                    }finally {
                        this.cancel();
                    }
                }
            }, poneSession.socketContext().getReadTimeout()/3/1000);
        }
        // CONTINUOUS 收到 pong 帧则返回 ping 帧
        else if(reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.CONTINUOUS){
            byteBufferChannel.writeEnd(reqWebSocketFrame.getFrameData());
        }
        // WS_RECIVE 文本和二进制消息触发 Recived 事件
        else if (reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.TEXT || reqWebSocketFrame.getOpcode() == WebSocketFrame.Opcode.BINARY) {
            Object result = null;

            byteBufferChannel.writeEnd(reqWebSocketFrame.getFrameData());

            try {
                //解包
                try {
                    result = webSocketRouter.filterDecoder(webSocketSession, byteBufferChannel.getByteBuffer());
                } finally {
                    byteBufferChannel.compact();
                    byteBufferChannel.clear();
                }

                //触发 onRecive
                result = webSocketRouter.onRecived(webSocketSession, result);

                if(result!=null) {
                    //封包
                    ByteBuffer buffer = (ByteBuffer) webSocketRouter.filterEncoder(webSocketSession, result);

                    respWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, true, buffer);
                }
            }catch (WebSocketFilterException e){
                Logger.error(e);
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
            try {
                obj = webSocketRouter.filterDecoder(webSocketSession, data);

                //触发 onSent
                webSocketRouter.onSent(webSocketSession, obj);
            } catch (WebSocketFilterException e) {
                Logger.error(e);
            }
        }
    }

    @Override
    public void onException(IoSession session, Exception e) {
        Logger.error(e);
    }

    @Override
    public void onIdle(IoSession session) {

    }
}
