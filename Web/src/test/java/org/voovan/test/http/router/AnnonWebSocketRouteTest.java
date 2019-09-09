package org.voovan.test.http.router;

import org.voovan.http.server.module.annontationRouter.annotation.WebSocket;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.filter.StringFilter;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
@WebSocket("ws")
public class AnnonWebSocketRouteTest extends WebSocketRouter {
    public AnnonWebSocketRouteTest() {
        this.addFilterChain(new StringFilter());
    }

    @Override
    public Object onOpen(WebSocketSession session) {
        System.out.println("connect");
        return null;
    }

    @Override
    public Object onRecived(WebSocketSession session, Object obj) {
        System.out.println("obj:" + obj);
        return System.currentTimeMillis();
    }

    @Override
    public void onSent(WebSocketSession session, Object obj) {

    }

    @Override
    public void onClose(WebSocketSession session) {

    }
}
