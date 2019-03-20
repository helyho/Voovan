package org.voovan.network;

import org.voovan.network.Event.EventName;
import org.voovan.network.exception.IoFilterException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.Chain;
import org.voovan.tools.TByteBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 事件的实际逻辑处理
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventProcess {
    public static ThreadLocal<ByteBuffer> THREAD_BYTE_BYTE = ThreadLocal.withInitial(()-> TByteBuffer.allocateDirect());

    /**
     * 私有构造函数,防止被实例化
     */
    private EventProcess(){

    }

    /**
     * Accept事件
     *
     * @param event
     *            事件对象
     * @throws IOException IO 异常
     */
    public static void onAccepted(Event event) throws IOException {
        SocketContext socketContext = event.getSession().socketContext();
        socketContext.acceptStart();
    }

    /**
     * 连接成功事件 建立连接完成后出发
     *
     * @param event 事件对象
     * @throws IOException IO 异常
     *
     */
    public static void onConnect(Event event) throws IOException {

        IoSession session = event.getSession();

        // SSL 握手
        if (session != null && session.getSSLParser() != null && !session.getSSLParser().isHandShakeDone()) {
            try {
                if (session.getSSLParser().doHandShake() &&
                        session.getReadByteBufferChannel().size() > 0) {

                    //将握手后的剩余数据进行处理, 并触发 onRecive 事件
                    ByteBufferChannel byteBufferChannel = new ByteBufferChannel();
                    session.getSSLParser().unWarpByteBufferChannel(session, session.getReadByteBufferChannel(), byteBufferChannel);
                    session.getReadByteBufferChannel().clear();

                    try {
                        session.getReadByteBufferChannel().writeHead(byteBufferChannel.getByteBuffer());
                    }finally {
                        byteBufferChannel.compact();
                        byteBufferChannel.release();
                    }

                    EventTrigger.fireReceive(session);
                }
            } catch (Exception e) {
                session.close();
                throw e;
            }
        }

        SocketContext socketContext = event.getSession().socketContext();
        if (socketContext != null && session != null) {
            Object original = socketContext.handler().onConnect(session);
            //null 不发送
            if (original != null) {
                sendMessage(session, original);
                session.flush();
            }
        }

        //设置空闲状态
        session.getState().setConnect(false);
    }

    /**
     * 连接断开事件 断开后出发
     *
     * @param event
     *            事件对象
     */
    public static void onDisconnect(Event event) {
        IoSession session = event.getSession();
        session.cancelIdle();
        SocketContext socketContext = event.getSession().socketContext();

        if (socketContext != null) {
            socketContext.handler().onDisconnect(session);
        }
    }

    /**
     * 读取事件 在消息接受完成后触发
     *
     * @param event
     *            事件对象
     * @param recursionDepth 递归深度控制
     * @throws SendMessageException  消息发送异常
     * @throws IOException  IO 异常
     * @throws IoFilterException IoFilter 异常
     */
    public static void onRead(Event event, int recursionDepth) throws IOException {
        IoSession session = event.getSession();
        int currentRecursionDepth = recursionDepth;

        if (session != null) {

            MessageLoader messageLoader = session.getMessageLoader();

            //如果没有使用分割器,则跳过
            if (!messageLoader.isEnable()) {
                return;
            }

            try {
                // 循环读取完整的消息包.
                // 由于之前有消息分割器在工作,所以这里读取的消息都是完成的消息包.
                // 有可能缓冲区没有读完
                // 按消息包触发 onRecive 事件
                int splitLength = messageLoader.read();

                if(splitLength>=0) {
                    Object result = doRecive(session, splitLength);

                    //如果有消息未处理完, 触发下一个 onRead
                    //通过读递归深度控制
                    recursionDepth++;
                    if (recursionDepth < session.socketContext().getReadRecursionDepth() && session.getReadByteBufferChannel().size() > 0) {
                        onRead(event, recursionDepth);
                    }
                }
            } finally {
                //释放 onRecive 锁
                if(currentRecursionDepth == 0) {

                    if(session.getSendByteBufferChannel().size() > 0) {
                        //异步处理 flush
                        session.flush();
                    }

                    if(session.getReadByteBufferChannel().size() > 0){
                        //如果还有数据继续触发 onReceive 事件
                        HeartBeat.interceptHeartBeat(session, session.getReadByteBufferChannel());
                        EventTrigger.fireReceiveThread(session);
                    }

                }
            }
        }
    }

    public static ByteBuffer loadSplitData(IoSession session, int splitLength) {

        if(splitLength == 0){
            return TByteBuffer.EMPTY_BYTE_BUFFER;
        }

        ByteBuffer byteBuffer = THREAD_BYTE_BYTE.get();
        byteBuffer.clear();

        //扩容线程本地变量
        if (byteBuffer.capacity() < splitLength) {
            TByteBuffer.reallocate(byteBuffer, splitLength);
        }

        try {
            byteBuffer.limit(splitLength);
        } catch (Exception e){
            e.printStackTrace();
        }

        if ((session.socketContext() instanceof UdpSocket && session.isOpen()) || session.isConnected()) {
            session.getReadByteBufferChannel().readHead(byteBuffer);
            return byteBuffer;
        } else {
            return null;
        }
    }

    /**
     * 接收的消息处理函数
     * @param session  会话对象
     * @param splitLength 分割有效自己数
     * @return 产生的响应
     * @throws IoFilterException 过滤器异常
     */
    public static Object doRecive(IoSession session, int splitLength) throws IOException {
        ByteBuffer byteBuffer = loadSplitData(session, splitLength);

        //如果读出的数据为 null 则直接返回
        if (byteBuffer == null) {
            return null;
        }

        Object result = null;

        // -----------------Filter 解密处理-----------------
        result = filterDecoder(session, byteBuffer);
        // -------------------------------------------------

        // -----------------Handler 业务处理-----------------
        if (result != null) {
            IoHandler handler = session.socketContext().handler();
            result = handler.onReceive(session, result);
        }
        // --------------------------------------------------

        // 返回的结果不为空的时候才发送
        if (result != null) {

            //触发发送事件
            sendMessage(session, result);
            return result;
        } else {
            return null;
        }
    }

    /**
     * 使用过滤器过滤解码结果
     * @param session      Session 对象
     * @param readedBuffer	   需解码的对象
     * @return  解码后的对象
     * @throws IoFilterException 过滤器异常
     */
    public static Object filterDecoder(IoSession session, ByteBuffer readedBuffer) throws IoFilterException{
        Object result = readedBuffer;
        Chain<IoFilter> filterChain = session.socketContext().filterChain().rewind();
        while (filterChain.hasNext()) {
            IoFilter fitler = filterChain.next();
            result = fitler.decode(session, result);
            if(result == null){
                break;
            }
        }

        return result;
    }

    /**
     * 使用过滤器编码结果
     * @param session      Session 对象
     * @param result	   需编码的对象
     * @return  编码后的对象
     * @throws IoFilterException 过滤器异常
     */
    public static ByteBuffer filterEncoder(IoSession session, Object result) throws IoFilterException{
        Chain<IoFilter> filterChain = session.socketContext().filterChain().rewind();
        filterChain.rewind();
        while (filterChain.hasPrevious()) {
            IoFilter fitler = filterChain.previous();
            result = fitler.encode(session, result);
            if(result == null){
                break;
            }
        }

        if(result instanceof ByteBuffer) {
            return (ByteBuffer)result;
        } else if(result==null){
            return null;
        } else{
            throw new IoFilterException("Send object must be ByteBuffer, " +
                    "please check you filter be sure the latest filter return Object's type is ByteBuffer.");
        }
    }

    /**
     * 在一个独立的线程中并行的发送消息
     *
     * @param session Session 对象
     * @param obj 待发送的对象
     */
    public static void sendMessage(IoSession session, Object obj) {

        final Object sendObj = obj;

        try {
            // ------------------Filter 加密处理-----------------
            ByteBuffer sendBuffer = EventProcess.filterEncoder(session, sendObj);
            // ---------------------------------------------------

            // 发送消息
            if (sendBuffer != null && session.isOpen()) {
                if (sendBuffer.limit() > 0) {
                    int sendLength = session.send(sendBuffer);
                    if(sendLength >= 0) {
                        sendBuffer.rewind();
                    } else {
                        throw new IOException("EventProcess.sendMessage faild, writeToChannel length: " + sendLength);
                    }
                }
            }

            //触发发送事件
            EventTrigger.fireSent(session, sendObj);

        } catch (IOException e) {
            EventTrigger.fireException(session, e);
        }
    }

    /**
     * 发送完成事件 发送后出发
     *
     * @param event
     *            事件对象
     * @param sendObj
     *            发送的对象
     * @throws IOException IO 异常
     */
    public static void onSent(Event event, Object sendObj) throws IOException {
        IoSession session = event.getSession();
        SocketContext socketContext = session.socketContext();
        if (socketContext != null) {
            socketContext.handler().onSent(session, sendObj);
        }
    }

    /**
     * 发送到 Socket 缓冲区事件
     *
     * @param event
     *            事件对象
     * @throws IOException IO 异常
     */
    public static void onFlush(Event event) throws IOException {
        IoSession session = event.getSession();
        SocketContext socketContext = session.socketContext();
        if (socketContext != null) {
            socketContext.handler().onFlush(session);
        }
    }

    /**
     * 空闲事件触发
     *
     * @param event 事件对象
     */
    public static void onIdle(Event event) {
        SocketContext socketContext = event.getSession().socketContext();
        if (socketContext != null) {
            IoSession session = event.getSession();

            socketContext.handler().onIdle(session);
        }
    }

    /**
     * 异常产生事件 异常产生侯触发
     *
     * @param event
     *            事件对象
     * @param e 异常对象
     */
    public static void onException(Event event, Exception e) {

        IoSession session = event.getSession();

        SocketContext socketContext = event.getSession().socketContext();
        if (socketContext != null) {
            if (socketContext.handler() != null) {
                socketContext.handler().onException(session, e);
            }
        }
    }

    /**
     * 处理异常
     * @param event 事件对象
     */
    public static void process(Event event) {
        if (event == null) {
            return;
        }
        EventName eventName = event.getName();
        // 根据事件名称处理事件
        try {
            if (eventName == EventName.ON_ACCEPTED) {
                EventProcess.onAccepted(event);
            } else if (eventName == EventName.ON_CONNECT) {
                EventProcess.onConnect(event);
            } else if (eventName == EventName.ON_DISCONNECT) {
                //设置空闲状态
                EventProcess.onDisconnect(event);
            } else if (eventName == EventName.ON_RECEIVE) {
                EventProcess.onRead(event, 0);
            } else if (eventName == EventName.ON_SENT) {
                EventProcess.onSent(event, event.getOther());
            } else if (eventName == EventName.ON_FLUSH) {
                EventProcess.onFlush(event);
            } else if (eventName == EventName.ON_IDLE) {
                EventProcess.onIdle(event);
            } else if (eventName == EventName.ON_EXCEPTION) {
                EventProcess.onException(event, (Exception)event.getOther());
            }
        } catch (Exception e) {
            EventProcess.onException(event, e);
        }
    }
}
