package org.voovan.test.tio;

import org.tio.core.Aio;
import org.tio.core.ChannelContext;
import org.tio.core.GroupContext;
import org.tio.core.exception.AioDecodeException;
import org.tio.core.intf.Packet;
import org.tio.server.AioServer;
import org.tio.server.ServerGroupContext;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TioServer {
    private class TioPacket extends Packet{

    }


    public static void main(String[] args) throws IOException {
        ServerGroupContext<Object, TioPacket, Object> serverGroupContext
                = new ServerGroupContext<Object, TioPacket, Object>(new ServerAioHandler(){
            @Override
            public Object handler(Packet packet, ChannelContext channelContext) throws Exception {
                Aio.send(channelContext, packet);
                return packet;
            }

            @Override
            public ByteBuffer encode(Packet packet, GroupContext groupContext, ChannelContext channelContext) {
                String retVal = "HTTP/1.1 200 OK\r\n" +
                        "Server: Voovan-WebServer/V1.0-RC-1\r\n" +
                        "Connection: keep-alive\r\n" +
                        "Content-Length: 2\r\n" +
                        "Date: Thu, 05 Jan 2017 04:55:20 GMT\r\n" +
                        "Content-Type: text/html\r\n"+
                        "\r\n"+
                        "OK\r\n\r\n";
                ByteBuffer byteBuffer = ByteBuffer.wrap(retVal.getBytes());
                byteBuffer.position(byteBuffer.limit());
                return byteBuffer;
            }

            @Override
            public Packet decode(ByteBuffer byteBuffer, ChannelContext channelContext) throws AioDecodeException {
                byteBuffer.position(byteBuffer.limit());
                Packet packet = new Packet();
                return packet;
            }
        }
        , new ServerAioListener(){
            @Override
            public void onAfterConnected(ChannelContext channelContext, boolean b, boolean b1) throws Exception {

            }

            @Override
            public void onAfterSent(ChannelContext channelContext, Packet packet, boolean b) throws Exception {
                Aio.close(channelContext, "close");
            }

            @Override
            public void onAfterReceived(ChannelContext channelContext, Packet packet, int i) throws Exception {

            }

            @Override
            public void onAfterClose(ChannelContext channelContext, Throwable throwable, String s, boolean b) throws Exception {

            }
        } );

        //aioServer对象
        AioServer<Object, TioPacket, Object> aioServer = new AioServer<>(serverGroupContext);

        //有时候需要绑定ip，不需要则null
        String serverIp = "127.0.0.1";

        //监听的端口
        int serverPort = 28080;

        aioServer.start(serverIp, serverPort);

    }
}
