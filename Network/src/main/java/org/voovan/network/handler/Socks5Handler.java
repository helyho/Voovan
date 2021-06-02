package org.voovan.network.handler;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.TEnv;
import org.voovan.tools.buffer.ByteBufferChannel;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Socks5Handler implements IoHandler {

    static final byte PROTO_VERS4        = (byte)4;
    static final byte PROTO_VERS         = (byte)5;
    static final byte DEFAULT_PORT       = (byte) 1080;

    static final byte NO_AUTH            = (byte)0;
    static final byte GSSAPI             = (byte)1;
    static final byte USER_PASSW         = (byte)2;
    static final byte NO_METHODS         = (byte)-1;

    static final byte CONNECT            = (byte)1;
    static final byte BIND               = (byte)2;
    static final byte UDP_ASSOC          = (byte)3;

    static final byte IPV4               = (byte)1;
    static final byte DOMAIN_NAME        = (byte)3;
    static final byte IPV6               = (byte)4;

    static final byte REQUEST_OK         = (byte)0;
    static final byte GENERAL_FAILURE    = (byte)1;
    static final byte NOT_ALLOWED        = (byte)2;
    static final byte NET_UNREACHABLE    = (byte)3;
    static final byte HOST_UNREACHABLE   = (byte)4;
    static final byte CONN_REFUSED       = (byte)5;
    static final byte TTL_EXPIRED        = (byte)6;
    static final byte CMD_NOT_SUPPORTED  = (byte)7;
    static final byte ADDR_TYPE_NOT_SUP  = (byte)8;

    private IoHandler handler;
    private InetSocketAddress endpoint;

    public Socks5Handler(SocketAddress endpoint, IoHandler handler) {
        this.handler = handler;
        this.endpoint = (InetSocketAddress) endpoint;
    }

    @Override
    public Object onConnect(IoSession session) {

        try {
            ByteBufferChannel sendByteBufferChannel = session.getSendByteBufferChannel();
            ByteBufferChannel readByteBufferChannel = session.getReadByteBufferChannel();

            ByteBuffer sendByteBuffer = ByteBuffer.allocate(512);
            sendByteBuffer.put(PROTO_VERS);
            sendByteBuffer.put((byte) 2);
            sendByteBuffer.put(NO_AUTH);
            sendByteBuffer.put(USER_PASSW);
            sendByteBuffer.flip();
            session.send(sendByteBuffer);
            session.flush();
            sendByteBuffer.clear();

            TEnv.wait(()->{
                session.socketSelector().select();
                return readByteBufferChannel.size()==0 && session.isConnected();
            });

            System.out.println("step 1 done");
            //================================ step 1 ================================
            int protoVer = readByteBufferChannel.get(0);
            if (protoVer != PROTO_VERS) {
               throw new SocketException("SOCKS : Version is not v5");
            }

            int authMethod = readByteBufferChannel.get(1);
            if (authMethod == NO_METHODS) {
                throw new SocketException("SOCKS : No acceptable methods");
            } else {
                authenticate(authMethod);
            }

            readByteBufferChannel.shrink(2);

            //================================ step 2 ================================
            sendByteBuffer.put((byte)PROTO_VERS);
            sendByteBuffer.put((byte)CONNECT);
            sendByteBuffer.put((byte)0);

            if (endpoint.isUnresolved()) {
                sendByteBuffer.put(DOMAIN_NAME);
                sendByteBuffer.put((byte) endpoint.getHostName().length());
                try {
                    sendByteBuffer.put(endpoint.getHostName().getBytes("ISO-8859-1"));
                } catch (java.io.UnsupportedEncodingException uee) {
                    assert false;
                }
                sendByteBuffer.put((byte) ((endpoint.getPort() >> 8) & 0xff));
                sendByteBuffer.put((byte) ((endpoint.getPort() >> 0) & 0xff));
            } else if (endpoint.getAddress() instanceof Inet6Address) {
                sendByteBuffer.put(IPV6);
                sendByteBuffer.put(endpoint.getAddress().getAddress());
                sendByteBuffer.put((byte) ((endpoint.getPort() >> 8) & 0xff));
                sendByteBuffer.put((byte) ((endpoint.getPort() >> 0) & 0xff));
            } else {
                sendByteBuffer.put(IPV4);
                sendByteBuffer.put(endpoint.getAddress().getAddress());
                sendByteBuffer.put((byte) ((endpoint.getPort() >> 8) & 0xff));
                sendByteBuffer.put((byte) ((endpoint.getPort() >> 0) & 0xff));
            }
            sendByteBuffer.flip();
            session.send(sendByteBuffer);
            session.flush();

            TEnv.wait(()->{
                session.socketSelector().select();
                return readByteBufferChannel.size()==0 && session.isConnected();
            });

            byte[] data = new byte[4];

            int readSize = readByteBufferChannel.get(data);
            if (readSize != 4) {
                throw new SocketException("Reply from SOCKS server has bad length");
            }

            readByteBufferChannel.shrink(4);

            SocketException ex = null;
            int len;
            byte[] addr;
            switch (data[1]) {
                case REQUEST_OK:
                    // success!
                    switch (data[3]) {
                        case IPV4:
                            addr = new byte[4];
                            int i = readByteBufferChannel.get(addr);
                            readByteBufferChannel.shrink(4);
                            if (i != 4)
                                throw new SocketException("Reply from SOCKS server badly formatted");
                            data = new byte[2];
                            i = readByteBufferChannel.get(data);
                            readByteBufferChannel.shrink(2);
                            if (i != 2)
                                throw new SocketException("Reply from SOCKS server badly formatted");
                            break;
                        case DOMAIN_NAME:
                            len = data[1];
                            byte[] host = new byte[len];
                            i = readByteBufferChannel.get(host);
                            readByteBufferChannel.shrink(len);
                            if (i != len)
                                throw new SocketException("Reply from SOCKS server badly formatted");
                            data = new byte[2];
                            i = readByteBufferChannel.get(data);
                            readByteBufferChannel.shrink(2);
                            if (i != 2)
                                throw new SocketException("Reply from SOCKS server badly formatted");
                            break;
                        case IPV6:
                            len = data[1];
                            addr = new byte[len];
                            i = readByteBufferChannel.get(addr);
                            readByteBufferChannel.shrink(len);
                            if (i != len)
                                throw new SocketException("Reply from SOCKS server badly formatted");
                            data = new byte[2];
                            i = readByteBufferChannel.get(data);
                            readByteBufferChannel.shrink(2);
                            if (i != 2)
                                throw new SocketException("Reply from SOCKS server badly formatted");
                            break;
                        default:
                            ex = new SocketException("Reply from SOCKS server contains wrong code");
                            break;
                    }
                    break;
                case GENERAL_FAILURE:
                    ex = new SocketException("SOCKS server general failure");
                    break;
                case NOT_ALLOWED:
                    ex = new SocketException("SOCKS: Connection not allowed by ruleset");
                    break;
                case NET_UNREACHABLE:
                    ex = new SocketException("SOCKS: Network unreachable");
                    break;
                case HOST_UNREACHABLE:
                    ex = new SocketException("SOCKS: Host unreachable");
                    break;
                case CONN_REFUSED:
                    ex = new SocketException("SOCKS: Connection refused");
                    break;
                case TTL_EXPIRED:
                    ex = new SocketException("SOCKS: TTL expired");
                    break;
                case CMD_NOT_SUPPORTED:
                    ex = new SocketException("SOCKS: Command not supported");
                    break;
                case ADDR_TYPE_NOT_SUP:
                    ex = new SocketException("SOCKS: address type not supported");
                    break;
            }
            if (ex != null) {
                throw ex;
            }

            String reqData = "GET / HTTP/1.1\r\n" +
                    "Host: www.voovan.org\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Cache-Control: max-age=0\r\n" +
                    "Upgrade-Insecure-Requests: 1\r\n" +
                    "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36\r\n" +
                    "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\r\n" +
                    "Accept-Encoding: gzip, deflate\r\n" +
                    "Accept-Language: zh-CN,zh;q=0.9,en;q=0.8\r\n" +
                    "Cookie: _ga=GA1.1.383879526.1585421546; _ga_NSJH3FW0LK=GS1.1.1620788011.10.1.1620788300.0\r\n\r\n";
            sendByteBufferChannel.writeEnd(reqData.getBytes(), 0, reqData.length());
            session.flush();

            TEnv.sleep(1000);

            TEnv.wait(()->{
                session.socketSelector().select();
                return readByteBufferChannel.size()==0 && session.isConnected();
            });

            byte[] respData = new byte[4096];

            int respSize = readByteBufferChannel.get(respData);

            System.out.println(new String(respData, 0, respSize));

            TEnv.sleep(1000);
            return null;
//            return handler.onConnect(session);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onDisconnect(IoSession session) {
        handler.onDisconnect(session);
    }

    @Override
    public Object onReceive(IoSession session, Object obj) {
        return handler.onReceive(session,obj);
    }

    @Override
    public void onSent(IoSession session, Object obj) {
        handler.onSent(session, obj);
    }

    @Override
    public void onFlush(IoSession session) {
        handler.onFlush(session);
    }

    @Override
    public void onException(IoSession session, Exception e) {
        e.printStackTrace();
        handler.onException(session, e);
    }

    @Override
    public void onIdle(IoSession session) {
        handler.onIdle(session);
    }

    public void authenticate(int authMethod) {

    }
}
