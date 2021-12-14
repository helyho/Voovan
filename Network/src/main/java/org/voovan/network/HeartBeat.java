package org.voovan.network;

import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.TEnv;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 心跳类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HeartBeat {
    private byte[] ping;
    private byte[] pong;
    private ByteBuffer pingBuffer;
    private ByteBuffer pongBuffer;
    private boolean isFirstBeat = true;
    private volatile boolean hasPing;
    private volatile boolean hasPong;
    private int failedCount = 0;


    /**
     * 构造方法
     * @param ping  ping 消息
     * @param pong  pong 消息
     * @return 心跳消息对象
     */
    private HeartBeat(String ping, String pong){
        this.ping = ping.getBytes();
        this.pong = pong.getBytes();
        this.pingBuffer = ByteBuffer.allocate(this.ping.length);
        this.pongBuffer = ByteBuffer.allocate(this.pong.length);

        this.hasPing = false;
        this.hasPong = false;

        pingBuffer.put(this.ping);
        pongBuffer.put(this.pong);
        pingBuffer.flip();
        pongBuffer.flip();
    }

    /**
     * 获取 ping 报文
     * @return ping 报文
     */
    public byte[] getPing() {
        return ping;
    }

    /**
     * 获取 pong 报文
     * @return pong 报文
     */
    public byte[] getPong() {
        return pong;
    }

    /**
     * 获取连续失败次数
     *      每次成功会被归零
     * @return 失败次数
     */
    public int getFailedCount() {
        return failedCount;
    }

    /**
     * 截断心跳消息
     * @param session 会话对象
     * @return true: 成功, false: 失败
     */
    public static boolean intercept(IoSession session){
        if(session==null || session.getHeartBeat()==null){
            return false;
        }

        HeartBeat heartBeat = session.getHeartBeat();
        ByteBufferChannel byteBufferChannel = session.getReadByteBufferChannel();

        if (byteBufferChannel.size() > 0) {
            //心跳处理
            if (heartBeat != null) {
                if (byteBufferChannel.startWith(heartBeat.getPing())) {
                    byteBufferChannel.shrink(0, heartBeat.getPing().length);
                    heartBeat.hasPing = true;
                    return true;
                }

                if (byteBufferChannel.startWith(heartBeat.getPong())) {
                    byteBufferChannel.shrink(0, heartBeat.getPong().length);
                    heartBeat.hasPong = true;
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 一次心跳动作
     * @param session 会话对象
     * @return true:心跳成功,false: 心跳失败
     */
    public static boolean beat(IoSession session) {
        HeartBeat heartBeat = session.getHeartBeat();

        if(!session.isConnected()){
            return false;
        }

        //首个心跳默认成功
        if (heartBeat.isFirstBeat) {
            //客户端默认发起第一次的心跳
            if (session.socketContext().getConnectModel() == ConnectModel.CLIENT) {
                heartBeat.hasPong = true;
            }

            heartBeat.isFirstBeat = false;
            return true;
        }

        if (heartBeat.hasPing) {
            session.send(heartBeat.pongBuffer);
            session.flush();
            heartBeat.pongBuffer.flip();
            heartBeat.failedCount = 0;
            heartBeat.hasPing = false;
            return true;
        } else if (heartBeat.hasPong) {
            session.send(heartBeat.pingBuffer);
            session.flush();
            heartBeat.pingBuffer.flip();
            heartBeat.failedCount = 0;
            heartBeat.hasPong = false;
            return true;
        } else {
            heartBeat.failedCount++;
            return false;
        }
    }
    /**
     * 将心跳绑定到 Session
     * @param session   会话
     * @param ping  ping 消息
     * @param pong  pong 消息
     * @return 心跳消息对象
     */
    public static HeartBeat attachSession(IoSession session, String ping, String pong){
        HeartBeat heartBeat = null;
        if(session.getHeartBeat()==null) {
            heartBeat = new HeartBeat(ping, pong);
            session.setHeartBeat(heartBeat);
        } else{
            heartBeat = session.getHeartBeat();
        }
        return heartBeat;

    }

    /**
     * 将心跳绑定到 Session
     *      默认使用 PING, PONG 作为心跳消息
     * @param session   Socket会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @return 心跳消息对象
     */
    public static HeartBeat attachSession(IoSession session, int connectModel){
        HeartBeat heartBeat = null;
        if(session.getHeartBeat()==null) {
            heartBeat = new HeartBeat("PING", "PONG");
            session.setHeartBeat(heartBeat);
        } else {
            heartBeat = session.getHeartBeat();
        }

        return heartBeat;
    }
}
