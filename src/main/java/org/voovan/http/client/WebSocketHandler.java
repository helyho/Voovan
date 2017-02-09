package org.voovan.http.client;

import org.voovan.http.server.websocket.WebSocketFrame;
import org.voovan.http.server.websocket.WebSocketTools;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.TObject;

import java.nio.ByteBuffer;

/**
 * 类文字命名
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
    public WebSocketHandler(HttpClient httpClient, WebSocketRouter webSocketRouter){
        this.webSocketRouter = webSocketRouter;
        this.httpClient = httpClient;
    }

    @Override
    public Object onConnect(IoSession session) {
        //不会被触发
        return null;
    }

    @Override
    public void onDisconnect(IoSession session) {
        webSocketRouter.onClose();
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {
        WebSocketFrame webSocketFrame = WebSocketFrame.parse(TObject.cast(obj));
        ByteBuffer data = webSocketRouter.onRecived(webSocketFrame.getFrameData());
        if(data!=null) {
            webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.BINARY, true, data);
        }else{
            webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, ByteBuffer.allocate(0));
        }
        return webSocketFrame;
    }

    @Override
    public void onSent(IoSession session, Object obj) {

    }

    @Override
    public void onException(IoSession session, Exception e) {

    }
}
