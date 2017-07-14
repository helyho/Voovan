package org.voovan.network;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HeartBeat {
    private int heartBeatInterval;
    private String ping;
    private String pong;
    private long lastModified;

    /**
     * 构造方法
     * @param session   会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @param heartBeatInterval 心跳间隔
     * @param ping  ping 消息
     * @param pong  pong 消息
     * @return 心跳消息对象
     */
    private HeartBeat(IoSession session, ConnectModel connectModel, int heartBeatInterval,  String ping, String pong){
        this.heartBeatInterval = heartBeatInterval;
        this.ping = ping;
        this.pong = pong;

        if(session.socketContext().getConnectModel() == connectModel){
            session.send(ByteBuffer.wrap(ping.getBytes()));
        }
        lastModified = System.currentTimeMillis();
    }

    /**
     * 判断会话中是否这个作为消息开始
     * @param msg 消息
     * @return true: 是, false: 否
     */
    private boolean isHeadOfMsg(IoSession session, String msg){
        ByteBufferChannel byteBufferChannel = session.getByteBufferChannel();

        if( byteBufferChannel.size() >= msg.length()){
            Logger.simple(byteBufferChannel.size());
            if(byteBufferChannel.indexOf(msg.getBytes()) == 0) {
                //收缩通道内的数据
                byteBufferChannel.shrink(msg.length());
                return true;
            }
        }

        return false;
    }

    /**
     * 检测是否是 Pong 消息
     * @return true: 是, false: 否
     */
    public boolean isPing(IoSession session){
        return isHeadOfMsg(session, ping);
    }

    /**
     * 检测是否是 Pong 消息
     * @return true: 是, false: 否
     */
    public boolean isPong(IoSession session){
        return isHeadOfMsg(session, pong);
    }

    /**
     *启动心跳驱动
     */
    public static void start(IoSession session){
        HeartBeat heartBeat = (HeartBeat)session.getAttribute("HEART_BEAT");

        //收到 PING 发送 PONG
        if(heartBeat.isPing(session)){
            session.getMessageLoader().reset();
            session.send(ByteBuffer.wrap(heartBeat.pong.getBytes()));
            heartBeat.lastModified = System.currentTimeMillis();
            Logger.simple("PING");
        }

        if(heartBeat.isPong(session)){
            TEnv.sleep(heartBeat.heartBeatInterval*1000);
            heartBeat.lastModified = System.currentTimeMillis();
            session.getMessageLoader().reset();
            session.send(ByteBuffer.wrap(heartBeat.ping.getBytes()));
            Logger.simple("PONG");
        }
    }

    /**
     * 检查心跳停止
     * @param session 会话
     * @param second 心跳停止的阀值
     * @return true: 停止,  false: 未停止
     */
    public static boolean checkHeartStop(IoSession session, int second){
        HeartBeat heartBeat = (HeartBeat)session.getAttribute("HEART_BEAT");
        long timeDiff = (System.currentTimeMillis() - heartBeat.lastModified)/1000;
        return second <= timeDiff;
    }

    /**
     * 构造一个心跳对象
     * @param session   会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @param heartBeatInterval 心跳间隔
     * @param ping  ping 消息
     * @param pong  pong 消息
     * @return 心跳消息对象
     */
    public static HeartBeat attachSession(IoSession session, ConnectModel connectModel, int heartBeatInterval, String ping, String pong){
        HeartBeat heartBeat = null;
        if(session.getAttribute("HEART_BEAT")==null) {
            heartBeat = new HeartBeat(session, connectModel, heartBeatInterval, ping, pong);
            session.setAttribute("HEART_BEAT", heartBeat);
        } else{
            heartBeat = (HeartBeat)session.getAttribute("HEART_BEAT");
        }
        return heartBeat;

    }

    /**
     * 构造一个心跳对象,默认使用 PING, PONG 作为消息
     * @param session   会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @param heartBeatInterval 心跳间隔
     * @return 心跳消息对象
     */
    public static HeartBeat attachSession(IoSession session, ConnectModel connectModel, int heartBeatInterval){
        HeartBeat heartBeat = null;
        if(session.getAttribute("HEART_BEAT")==null) {
            heartBeat = new HeartBeat(session, connectModel, heartBeatInterval, "PING", "PONG");
            session.setAttribute("HEART_BEAT", heartBeat);
        }else{
            heartBeat = (HeartBeat)session.getAttribute("HEART_BEAT");
        }

        return heartBeat;
    }
}
