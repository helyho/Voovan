package org.voovan.network;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;
import java.time.temporal.IsoFields;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HeartBeat {
    private String ping;
    private String pong;
    private boolean isFirstBeat = true;

    /**
     * 构造方法
     * @param session   会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @param ping  ping 消息
     * @param pong  pong 消息
     * @return 心跳消息对象
     */
    private HeartBeat(IoSession session, ConnectModel connectModel, String ping, String pong){
        this.ping = ping;
        this.pong = pong;

        if(session.socketContext().getConnectModel() == connectModel){
            session.send(ByteBuffer.wrap(ping.getBytes()));
        }
    }

    /**
     * 判断会话中是否这个作为消息开始
     * @param msg 消息
     * @return true: 是, false: 否
     */
    private boolean isHeadOfMsg(IoSession session, String msg){
        ByteBufferChannel byteBufferChannel = session.getByteBufferChannel();

        if( byteBufferChannel.size() >= msg.length()){
            if(byteBufferChannel.indexOf(msg.getBytes()) == 0) {
                //收缩通道内的数据
                byteBufferChannel.shrink(msg.length());
                try {
                    return true;
                }finally {
                    if(byteBufferChannel.size()!=0) {
                        while (isHeadOfMsg(session, msg)) {
                            continue;
                        }
                    }
                }
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
     * 一次心跳动作
     */
    public static boolean beat(IoSession session){


        HeartBeat heartBeat = (HeartBeat)session.getAttribute("HEART_BEAT");

        if(heartBeat.isFirstBeat){
            heartBeat.isFirstBeat=false;
            return true;
        }
        //收到 PING 发送 PONG
        if(heartBeat.isPing(session)){
            session.getMessageLoader().reset();
            session.send(ByteBuffer.wrap(heartBeat.pong.getBytes()));
            return true;
        } else if(heartBeat.isPong(session)){
            session.getMessageLoader().reset();
            session.send(ByteBuffer.wrap(heartBeat.ping.getBytes()));
            return true;
        } else {
            return false;
        }
    }

    /**
     * 将心跳绑定到 Session
     * @param session   会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @param ping  ping 消息
     * @param pong  pong 消息
     * @return 心跳消息对象
     */
    public static HeartBeat attachSession(IoSession session, ConnectModel connectModel, String ping, String pong){
        HeartBeat heartBeat = null;
        if(session.getAttribute("HEART_BEAT")==null) {
            heartBeat = new HeartBeat(session, connectModel, ping, pong);
            session.setAttribute("HEART_BEAT", heartBeat);
        } else{
            heartBeat = (HeartBeat)session.getAttribute("HEART_BEAT");
        }
        return heartBeat;

    }

    /**
     * 将心跳绑定到 Session
     *      默认使用 PING, PONG 作为心跳消息
     * @param session   会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @return 心跳消息对象
     */
    public static HeartBeat attachSession(IoSession session, ConnectModel connectModel){
        HeartBeat heartBeat = null;
        if(session.getAttribute("HEART_BEAT")==null) {
            heartBeat = new HeartBeat(session, connectModel, "PING", "PONG");
            session.setAttribute("HEART_BEAT", heartBeat);
        }else{
            heartBeat = (HeartBeat)session.getAttribute("HEART_BEAT");
        }

        return heartBeat;
    }
}
