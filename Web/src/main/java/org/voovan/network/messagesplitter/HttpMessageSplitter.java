package org.voovan.network.messagesplitter;

import org.voovan.Global;
import org.voovan.http.HttpSessionParam;
import org.voovan.http.message.HttpParser;
import org.voovan.http.HttpRequestType;
import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TByteBuffer;

import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Http 消息分割类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpMessageSplitter implements MessageSplitter {

    @Override
    public int canSplite(IoSession session, ByteBuffer byteBuffer) {
        int result = -1;

        //返回 -1: 报文未完整接受, -2: 报文异常, 管理连接
        if(byteBuffer.limit()==0){
            return -1;
        }

        if(HttpRequestType.WEBSOCKET.equals(session.getAttribute(HttpSessionParam.TYPE)) ){
            result = isWebSocketFrame(byteBuffer);
        }else{
            result = isHttpFrame(byteBuffer);

            if(result >= 0){
                if (!session.containAttribute(HttpSessionParam.TYPE)) {
                    session.setAttribute(HttpSessionParam.TYPE, HttpRequestType.HTTP);
                }
            } else if(result == -1){
                return result;
            } else if(result == -2){
//                采用异步的方式, 防止导致死锁
                Global.getThreadPool().execute(new Thread("CHECK_HTTP_HEAD_FAILED") {
                    @Override
                    public void run() {
                        session.close();
                    }
                });
            }
        }

        return result;
    }

    private int isHttpFrame(ByteBuffer byteBuffer){
        int bodyTagIndex = -1;
        int protocolLineIndex = -1;

        bodyTagIndex = TByteBuffer.indexOf(byteBuffer, HttpParser.BODY_MARK.getBytes());

        if(bodyTagIndex <= 0){
            return -1;
        }

        protocolLineIndex = TByteBuffer.indexOf(byteBuffer,  HttpParser.LINE_MARK.getBytes());
        if(protocolLineIndex <= 0){
            return -1;
        }

        if(TByteBuffer.indexOf(byteBuffer, "HTTP".getBytes()) > protocolLineIndex){
            return -1;
        }

        if(protocolLineIndex != -1) {
            if(bodyTagIndex > 0) {
                //兼容 http 的 pipeline 模式,  GET 请求直接返回指定的长度
                if(byteBuffer.get(0)=='G' && byteBuffer.get(1)=='E' && byteBuffer.get(2)=='T') {
                    return bodyTagIndex + 4;
                } else {
                    return 0;
                }
            } else {
                return -1;
            }

        }else{
            return -2;
        }
    }

    private boolean isHttpHead(String str){
        //判断是否是 HTTP 头
        if (str.startsWith(HttpParser.HTTP) || str.endsWith(HttpParser.HTTP)) {
            return true;
        }
        return false;
    }

    /**
     * 判断缓冲区中的数据是否是一个 WebSocket 帧
     * @param buffer 缓冲区对象
     * @return WebSocket 帧报文长度,-1不是WebSocket 帧, 大于0 返回的 WebSocket 的长度
     */
    public static int isWebSocketFrame(ByteBuffer buffer) {
        // 接受数据的大小
        int maxpacketsize = buffer.remaining();
        // 期望数据包的实际大小
        int expectPackagesize = 2;
        if (maxpacketsize < expectPackagesize) {
            return -2;
        }
        byte finByte = buffer.get();
        boolean fin = finByte >> 8 != 0;
        byte rsv = (byte) ((finByte & ~(byte) 128) >> 4);
        if (rsv != 0) {
            return -2;
        }
        byte maskByte = buffer.get();
        boolean mask = (maskByte & -128) != 0;
        int payloadlength = (byte) (maskByte & ~(byte) 128);
        int optcode = (byte) (finByte & 15);

        if(optcode < 0 && optcode > 10){
            return -2;
        }

        if (!fin) {
            if (optcode == 9 || optcode == 10 || optcode == 8) {
                return -2;
            }
        }

        if (payloadlength >= 0 && payloadlength <= 125) {
        } else {
            if (optcode == 9 || optcode == 10 || optcode == 8) {
                return -2;
            }
            if (payloadlength == 126) {
                expectPackagesize += 2;
                byte[] sizebytes = new byte[3];
                sizebytes[1] = buffer.get();
                sizebytes[2] = buffer.get();
                payloadlength = new BigInteger(sizebytes).intValue();
            } else {
                expectPackagesize += 8;
                byte[] bytes = new byte[8];
                for (int i = 0; i < 8; i++) {
                    bytes[i] = buffer.get();
                }
                long length = new BigInteger(bytes).longValue();
                if (length <= Integer.MAX_VALUE) {
                    payloadlength = (int) length;
                }
            }
        }

        expectPackagesize += (mask ? 4 : 0);
        expectPackagesize += payloadlength;

        // 如果实际接受的数据小于数据包的大小则报错
        if (maxpacketsize < expectPackagesize) {
            buffer.position(0);
            return -2;
        } else {
            buffer.position(0);
            return expectPackagesize;
        }
    }
}
