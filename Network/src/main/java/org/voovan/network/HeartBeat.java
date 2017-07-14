package org.voovan.network;

import org.voovan.tools.TByteBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HeartBeat {
    private ByteBuffer ping;
    private ByteBuffer pong;
    private boolean isFirstBeat = true;
    private LinkedBlockingDeque<Integer> queue;
    private int fieldCount = 0;


    /**
     * 构造方法
     * @param session   会话
     * @param connectModel  模式,指定发送第一个 PING 的模式,服务端先发,或者客户端先发
     * @param ping  ping 消息
     * @param pong  pong 消息
     * @return 心跳消息对象
     */
    private HeartBeat(IoSession session, ConnectModel connectModel, String ping, String pong){
        this.ping = ByteBuffer.wrap(ping.getBytes());
        this.pong = ByteBuffer.wrap(pong.getBytes());
        queue = new LinkedBlockingDeque<Integer>();

        if(session.socketContext().getConnectModel() == connectModel){
            session.send(ByteBuffer.wrap(ping.getBytes()));
        }
    }

    /**
     * 获取心跳包队列
     * @return 心跳包队列
     */
    private LinkedBlockingDeque<Integer> getQueue() {
        return queue;
    }

    /**
     * 获取 ping 报文
     * @return ping 报文
     */
    public ByteBuffer getPing() {
        return ping;
    }

    /**
     * 获取 pong 报文
     * @return pong 报文
     */
    public ByteBuffer getPong() {
        return pong;
    }

    /**
     * 获取连续失败次数
     *      每次成功会被归零
     * @return 失败次数
     */
    public int getFieldCount() {
        return fieldCount;
    }

//    /**
//     * 判断会话中是否这个作为消息开始
//     * @param msg 消息
//     * @return true: 是, false: 否
//     */
//    private boolean isHeadOfMsg(IoSession session, ByteBuffer msg){
//        ByteBufferChannel byteBufferChannel = session.getByteBufferChannel();
//
//        if( byteBufferChannel.size() >= msg.capacity()){
//            if(byteBufferChannel.indexOf(msg.array()) == 0) {
//                //收缩通道内的数据
//                byteBufferChannel.shrink(msg.capacity());
//                try {
//                    return true;
//                }finally {
//                    if(byteBufferChannel.size()!=0) {
//                        while (isHeadOfMsg(session, msg)) {
//                            continue;
//                        }
//                    }
//                }
//            }
//        }
//
//        return false;
//    }
//
//    /**
//     * 检测是否是 Pong 消息
//     * @return true: 是, false: 否
//     */
//    public boolean isPing(IoSession session){
//        return isHeadOfMsg(session, ping);
//    }
//
//    /**
//     * 检测是否是 Pong 消息
//     * @return true: 是, false: 否
//     */
//    public boolean isPong(IoSession session){
//        return isHeadOfMsg(session, pong);
//    }
//
//    /**
//     * 一次心跳动作
//     */
//    public static boolean beat_old(IoSession session){
//
//        HeartBeat heartBeat = session.getHeartBeat();
//
//        if(heartBeat.isFirstBeat){
//            heartBeat.isFirstBeat=false;
//            return true;
//        }
//        //收到 PING 发送 PONG
//        if(heartBeat.isPing(session)){
//            session.getMessageLoader().reset();
//            session.send(heartBeat.pong);
//            return true;
//        } else if(heartBeat.isPong(session)){
//            session.getMessageLoader().reset();
//            session.send(heartBeat.ping);
//            return true;
//        } else {
//            return false;
//        }
//    }

    /**
     * 截断心跳消息
     * @param session 会话对象
     * @param byteBuffer 保存消息 ByteBuffer 对象
     */
    public static void interceptHeartBeat(IoSession session, ByteBuffer byteBuffer){
        if(session==null || byteBuffer==null){
            return;
        }

        HeartBeat heartBeat = session.getHeartBeat();
        if(heartBeat!=null) {
            if (byteBuffer.hasRemaining()) {
                //心跳处理
                if (heartBeat != null) {
                    if (TByteBuffer.indexOf(byteBuffer, heartBeat.getPing().array()) == 0) {
                        if(byteBuffer.remaining() != heartBeat.getPing().limit()) {
                            TByteBuffer.moveData(byteBuffer, heartBeat.getPing().limit());
                        }
                        heartBeat.getQueue().addLast(1);
                    }
                    if (TByteBuffer.indexOf(byteBuffer, heartBeat.getPong().array()) == 0) {
                        if(byteBuffer.remaining() != heartBeat.getPing().limit()) {
                            TByteBuffer.moveData(byteBuffer, heartBeat.getPong().limit());
                        }
                        heartBeat.getQueue().addLast(2);
                    }
                }
            }
        }
    }

    /**
     * 一次心跳动作
     */
    public static boolean beat(IoSession session){

        HeartBeat heartBeat = session.getHeartBeat();

        //收个心跳返回成功
        if(heartBeat.isFirstBeat){
            heartBeat.isFirstBeat=false;
            return true;
        }

        if(heartBeat.getQueue().size() > 0) {
            int beatType = heartBeat.getQueue().pollFirst();

            if (beatType == 1) {
                session.getMessageLoader().reset();
                session.send(heartBeat.pong);
                heartBeat.fieldCount = 0;
                return true;
            } else if (beatType == 2) {
                session.getMessageLoader().reset();
                session.send(heartBeat.ping);
                heartBeat.fieldCount = 0;
                return true;
            } else {
                heartBeat.fieldCount++;
                return false;
            }
        }

        heartBeat.fieldCount++;
        return false;
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
        if(session.getHeartBeat()==null) {
            heartBeat = new HeartBeat(session, connectModel, ping, pong);
            session.setHeartBeat(heartBeat);
        } else{
            heartBeat = session.getHeartBeat();
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
        if(session.getHeartBeat()==null) {
            heartBeat = new HeartBeat(session, connectModel, "PING", "PONG");
            session.setHeartBeat(heartBeat);
        }else{
            heartBeat = session.getHeartBeat();
        }

        return heartBeat;
    }
}
