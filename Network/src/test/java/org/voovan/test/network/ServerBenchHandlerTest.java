package org.voovan.test.network;


import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

public class ServerBenchHandlerTest implements IoHandler {

    String respStr = "HTTP/1.1 200 OK\r\n" +
            "Server: Voovan-WebServer/V1.0-RC-1\r\n" +
            "Connection: keep-alive\r\n" +
            "Content-Length: 3\r\n" +
            "Date: Thu, 05 Jan 2017 04:55:20 GMT\r\n" +
            "Content-Type: text/html\r\n" +
            "\r\n" +
            "OK1\r\n\r\n";

    @Override
    public Object onConnect(IoSession session) {
        return null;
    }

    @Override
    public void onDisconnect(IoSession session) {
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {

        return ByteBuffer.wrap(respStr.getBytes());
    }

    @Override
    public void onException(IoSession session, Exception e) {
        Logger.error("Server exception", e);
        session.close();
    }

    @Override
    public void onIdle(IoSession session) {
        Logger.info("idle");
    }

    @Override
    public void onSent(IoSession session, Object obj) {
//        session.close();
    }

}
