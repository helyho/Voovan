package org.voovan.network;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;

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
    private byte[] ping;
    private byte[] pong;
    private ConnectModel connectModel;
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
        this.ping = ping.getBytes();
        this.pong = pong.getBytes();
        queue = new LinkedBlockingDeque<Integer>();
        this.connectModel = connectModel;
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
        return fieldCount;
    }

    /**
     * 截断心跳消息
     * @param session 会话对象
     * @param byteBufferChannel 保存消息 ByteBufferChannel 对象
     */
    public static void interceptHeartBeat(IoSession session, ByteBufferChannel byteBufferChannel){
        if(session==null || byteBufferChannel==null){
            return;
        }

        HeartBeat heartBeat = session.getHeartBeat();
        if (heartBeat != null && byteBufferChannel.size() > 0) {
            //心跳处理
            if (heartBeat != null) {
                int pingPosition = byteBufferChannel.indexOf(heartBeat.getPing());
                if (pingPosition >= 0) {
                    byteBufferChannel.shrink(pingPosition, heartBeat.getPing().length);
                    heartBeat.getQueue().addLast(1);
                    return;
                }

                int pongPosition = byteBufferChannel.indexOf(heartBeat.getPong());
                if (pongPosition >= 0) {
                    byteBufferChannel.shrink(pongPosition, heartBeat.getPing().length);
                    heartBeat.getQueue().addLast(2);
                    return;
                }
            }
        }
    }

    /**
     * 一次心跳动作
     * @param session 会话对象
     * @return true:心跳成功,false: 心跳失败
     */
    public static boolean beat(IoSession session){

        HeartBeat heartBeat = session.getHeartBeat();

        //收个心跳返回成功
        if(heartBeat.isFirstBeat){
            heartBeat.isFirstBeat=false;
            if(session.socketContext().getConnectModel() == ConnectModel.CLIENT){
                //等待这个时间的目的是为了等待客户端那边的心跳检测启动
                TEnv.sleep(session.getIdleInterval());
                session.send(ByteBuffer.wrap(heartBeat.ping));
            }
            return true;
        }

        //弥补双方发送的时间差,等待心跳到来,如果超过空闲事件周期则认为是失败
        int waitCount = 0;
        while(heartBeat.getQueue().size() == 0){
            session.wait(1);
            waitCount++;
            if(waitCount >= session.getIdleInterval()*1000){
                break;
            }
        }

        if(heartBeat.getQueue().size() > 0) {
            int beatType = heartBeat.getQueue().pollFirst();

            if (beatType == 1) {
                session.send(ByteBuffer.wrap(heartBeat.pong));
                heartBeat.fieldCount = 0;
                return true;
            } else if (beatType == 2) {
                session.send(ByteBuffer.wrap(heartBeat.ping));
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
     * @param session   Socket会话
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
