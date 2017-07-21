package org.voovan.http.extend.engineio;

import org.voovan.http.extend.socketio.SIOPacket;

/**
 * engine.io 报文
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EIOPacket {
    public static final int OPEN = 0;
    public static final int CLOSE = 1;
    public static final int PING = 2;
    public static final int PONG = 3;
    public static final int MESSAGE = 4;
    public static final int UPGRADE = 5;
    public static final int NOOP = 6;

    private String data = null;
    private int engineType = -1;

    public static String[] ENGINE_TYPES = new String[] {
            "OPEN",
            "CLOSE",
            "PING",
            "PONG",
            "MESSAGE",
            "UPGRADE",
            "NOOP"
    };

    public EIOPacket() {
    }

    public EIOPacket(int engineType, String data) {
        this.data = data;
        this.engineType = engineType;
    }

    public EIOPacket(int engineType, SIOPacket socketIOPacket) {
        this.engineType = engineType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getEngineType() {
        return engineType;
    }

    public void setEngineType(int engineType) {
        this.engineType = engineType;
    }
}
