package org.voovan.http.extend.engineio;

import org.voovan.http.extend.ParserException;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.filter.StringFilter;
import org.voovan.tools.log.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EIODispatcher extends WebSocketRouter{
    private Config config;
    private Map<String, EIOHandler> eioEventHandlers;

    public EIODispatcher(Config config){
        this.config = config;
        this.eioEventHandlers = new HashMap<String, EIOHandler>();
        this.addFilterChain(new StringFilter());
    }

    public EIODispatcher on(String Event, EIOHandler eioHandler){
        eioEventHandlers.put(Event, eioHandler);
        return this;
    }

    @Override
    public Object onOpen(WebSocketSession session) {
        EIOHandler eioHandler = eioEventHandlers.get("connection");
        if(eioHandler !=null) {
            eioHandler.setWebSocketSession(session);
            eioHandler.execute(null);
        }
        return EIOParser.encode(new EIOPacket(0, config.toString()));
    }

    @Override
    public Object onRecived(WebSocketSession session, Object obj) {
        try {
            if(EIOParser.isEngineIOMessage((String)obj)){
                EIOPacket eioPacket = EIOParser.decode((String)obj);

                if(eioPacket.getEngineType() == EIOPacket.MESSAGE){
                    EIOHandler eioHandler = eioEventHandlers.get("message");
                    if(eioHandler !=null) {
                        eioHandler.setWebSocketSession(session);
                        String result = eioHandler.execute(eioPacket.getData());
                        if(result!=null) {
                            eioPacket.setData(result);
                            return EIOParser.encode(eioPacket);
                        }
                    }
                }else if(eioPacket.getEngineType() == EIOPacket.PING){
                    EIOHandler eioHandler = eioEventHandlers.get("ping");
                    if(eioHandler !=null) {
                        eioHandler.setWebSocketSession(session);
                        eioHandler.execute(eioPacket.getData());
                    }
                    eioPacket.setEngineType(EIOPacket.PONG);
                    return EIOParser.encode(eioPacket);
                }else if(eioPacket.getEngineType() == EIOPacket.PONG){
                    EIOHandler eioHandler = eioEventHandlers.get("pong");
                    if(eioHandler !=null) {
                        eioHandler.setWebSocketSession(session);
                        eioHandler.execute(eioPacket.getData());
                    }
                    eioPacket.setEngineType(EIOPacket.PING);
                    return EIOParser.encode(eioPacket);
                }else if(eioPacket.getEngineType() == EIOPacket.NOOP){
                    EIOHandler eioHandler = eioEventHandlers.get("noop");
                    if(eioHandler !=null) {
                        eioHandler.setWebSocketSession(session);
                        String result = eioHandler.execute(null);
                        return EIOParser.encode(new EIOPacket(EIOPacket.MESSAGE, result));
                    }
                }

                return null;
            }
        } catch (ParserException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onSent(WebSocketSession session, Object obj) {

    }

    @Override
    public void onClose(WebSocketSession session) {
        EIOHandler eioHandler = eioEventHandlers.get("close");
        if(eioHandler !=null) {
            eioHandler.setWebSocketSession(session);
            eioHandler.execute(null);
        }
    }
}
