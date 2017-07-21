package org.voovan.http.extend.socketio;

import org.voovan.http.extend.engineio.Config;
import org.voovan.http.extend.engineio.EIODispatcher;
import org.voovan.http.extend.engineio.EIOHandler;
import org.voovan.tools.json.JSON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SIODispatcher extends EIODispatcher {

    private Map<String, SIOHandler> sioEventHandlers;

    public SIODispatcher(Config config) {
        super(config);
        this.sioEventHandlers = new HashMap<String, SIOHandler>();
        super.on("message", new MessageHandler(this));
    }

    public SIODispatcher on(String Event, SIOHandler sioHandler){
        sioEventHandlers.put(Event, sioHandler);
        return this;
    }

    public class MessageHandler extends EIOHandler {
        private SIODispatcher sioDispatcher;

        public MessageHandler(SIODispatcher sioDispatcher){
            this.sioDispatcher = sioDispatcher;
        }

        public MessageHandler() {

        }

        @Override
        public String execute(String msg) {
            try {
                if (SIOParser.isSocketIOMessage(msg)) {
                    SIOPacket sioPacket = SIOParser.decode(msg);

                    if (sioPacket.getSocketType() == SIOPacket.CONNECT) {
                        SIOHandler sioHandler = sioEventHandlers.get("connect");
                        sioHandler.setNsp(sioPacket.getNsp());
                        sioHandler.setEioHandler(this);
                        sioHandler.setSioDispatcher(sioDispatcher);
                        if (sioHandler != null) {
                            sioHandler.execute(null);
                        }

                        return SIOParser.encode(sioPacket);
                    }

                    if (sioPacket.getSocketType() == SIOPacket.DISCONNECT) {
                        SIOHandler sioHandler = sioEventHandlers.get("disconnect");
                        sioHandler.setNsp(sioPacket.getNsp());
                        sioHandler.setEioHandler(this);
                        sioHandler.setSioDispatcher(sioDispatcher);
                        if (sioHandler != null) {
                            sioHandler.execute(null);
                        }

                        return SIOParser.encode(sioPacket);
                    }

                    if (sioPacket.getSocketType() == SIOPacket.ERROR) {
                        SIOHandler sioHandler = sioEventHandlers.get("error");
                        sioHandler.setNsp(sioPacket.getNsp());
                        sioHandler.setEioHandler(this);
                        sioHandler.setSioDispatcher(sioDispatcher);
                        if (sioHandler != null) {
                            sioHandler.execute(null);
                        }

                        return SIOParser.encode(sioPacket);
                    }


                    if (sioPacket.getSocketType() == SIOPacket.EVENT) {
                        List argsList = (List) JSON.parse(sioPacket.getData());
                        String event = (String) argsList.get(0);
                        argsList.remove(0);
                        Object[] args = argsList.toArray(new Object[]{});

                        SIOHandler sioHandler = sioEventHandlers.get(event);
                        sioHandler.setNsp(sioPacket.getNsp());
                        sioHandler.setEioHandler(this);
                        sioHandler.setSioDispatcher(sioDispatcher);
                        if (sioHandler != null) {
                            Object result = sioHandler.execute(args);

                            if(result!=null) {
                                if (!result.getClass().isArray()) {
                                    result = new Object[]{result};
                                }

                                //设置成 ACK 事件
                                sioPacket.setSocketType(3);
                                sioPacket.setData(JSON.toJSON(result));
                                return SIOParser.encode(sioPacket);
                            }
                        }
                    }

                    if (sioPacket.getSocketType() == SIOPacket.ACK) {
                        List argsList = (List) JSON.parse(sioPacket.getData());
                        String event = (String) argsList.get(0);
                        argsList.remove(0);
                        Object[] args = argsList.toArray(new Object[]{});

                        SIOHandler sioHandler = sioEventHandlers.get("ack_" + event);
                        sioHandler.setNsp(sioPacket.getNsp());
                        sioHandler.setEioHandler(this);
                        sioHandler.setSioDispatcher(sioDispatcher);
                        if (sioHandler != null) {
                            sioHandler.execute(args);
                        }
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }


}
