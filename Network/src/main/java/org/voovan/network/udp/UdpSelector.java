package org.voovan.network.udp;

import org.voovan.Global;
import org.voovan.network.*;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeoutException;

/**
 * UDP事件监听器
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UdpSelector {

    public static ConcurrentLinkedDeque<UdpSelector> SELECTORS = new ConcurrentLinkedDeque<UdpSelector>();
    static {
        Global.getThreadPool().execute(()->{
            while(true){
                UdpSelector udpSelector = SELECTORS.poll();
                if(udpSelector!=null && udpSelector.socketContext.isOpen()) {
                    udpSelector.eventChose();
                    SELECTORS.offer(udpSelector);
                }
            }
        });
    }

    public static void register(UdpSelector selector){
        SELECTORS.add(selector);
    }

    public static void unregister(UdpSelector selector){
        SELECTORS.remove(selector);
    }

    private Selector selector;
    private SocketContext socketContext;
    private ByteBufferChannel appByteBufferChannel;
    private UdpSession session;
    private ByteBuffer readTempBuffer;

    /**
     * 事件监听器构造
     * @param selector   对象Selector
     * @param socketContext socketContext 对象
     */
    public UdpSelector(Selector selector, SocketContext socketContext) {
        this.selector = selector;
        this.socketContext = socketContext;
        if (socketContext instanceof UdpSocket){
            session = ((UdpSocket)socketContext).getSession();
            appByteBufferChannel = session.getReadByteBufferChannel();
        }

        readTempBuffer = TByteBuffer.allocateDirect(socketContext.getBufferSize());
    }

    /**
     * 所有的事件均在这里触发
     */
    public void eventChose() {
        if (socketContext instanceof UdpSocket) {
            // 连接完成onConnect事件触发
        }

        // 事件循环
        try {
            if (socketContext != null && socketContext.isOpen()) {
                if (selector.selectNow() > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> selectionKeyIterator = selectionKeys.iterator();
                    while (selectionKeyIterator.hasNext()) {
                        SelectionKey selectionKey = selectionKeyIterator.next();
                        if (selectionKey.isValid()) {
                            // 获取 socket 通道
                            DatagramChannel datagramChannel = getDatagramChannel(selectionKey);
                            if (datagramChannel.isOpen() && selectionKey.isValid()) {
                                // 事件分发,包含时间 onRead onAccept
                                Global.getThreadPool().submit(()->{
                                    try {
                                        switch (selectionKey.readyOps()) {

                                            // 有数据读取
                                            case SelectionKey.OP_READ: {

                                                int readSize = - 1;
                                                UdpSocket clientUdpSocket = null;

                                                //接受的连接isConnected 是 false
                                                //发起的连接isConnected 是 true
                                                if(datagramChannel.isConnected()) {
                                                    readSize = datagramChannel.read(readTempBuffer);
                                                }else{
                                                    SocketAddress address = datagramChannel.receive(readTempBuffer);
                                                    readSize = readTempBuffer.position();
                                                    clientUdpSocket = new UdpSocket(socketContext, datagramChannel, (InetSocketAddress)address);
                                                    session = clientUdpSocket.getSession();
                                                    appByteBufferChannel = session.getReadByteBufferChannel();
                                                    //触发连接时间, 关闭事件在触发 onSent 之后触发
                                                    EventTrigger.fireConnect(session);
                                                }

                                                //判断连接是否关闭
                                                if (MessageLoader.isStreamEnd(readTempBuffer, readSize) && session.isConnected()) {
                                                    session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
                                                    session.close();
                                                    break;
                                                } else if (readSize > 0) {
                                                    readTempBuffer.flip();

                                                    //如果缓冲队列已慢, 则等待可用, 超时时间为读超时
                                                    try {
                                                        TEnv.wait(session.socketContext().getReadTimeout(), () -> appByteBufferChannel.size() + readTempBuffer.limit() >= appByteBufferChannel.getMaxSize());
                                                    } catch (TimeoutException e) {
                                                        Logger.error("Session.byteByteBuffer is not enough:", e);
                                                    }

                                                    appByteBufferChannel.writeEnd(readTempBuffer);

                                                    if(appByteBufferChannel.size() > 0) {
                                                        // 触发 onReceive 事件
                                                        EventTrigger.fireReceiveThread(session);
                                                    }

                                                    readTempBuffer.clear();
                                                }
                                                break;
                                            } default: {
                                                Logger.fremawork("Nothing to do ,SelectionKey is:" + selectionKey.readyOps());
                                            }
                                        }
                                        selectionKeyIterator.remove();
                                    } catch (Exception e) {
                                        if(e instanceof IOException){
                                            session.close();
                                        }
                                        //兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
                                        else if(e.getStackTrace()[0].getClassName().contains("sun.nio.ch")){
                                            return;
                                        } else if(e instanceof Exception){
                                            //触发 onException 事件
                                            EventTrigger.fireExceptionThread(session, e);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // 触发 onException 事件
            EventTrigger.fireExceptionThread(session, e);
        }
    }

    /**
     * 获取 socket 通道
     *
     * @param selectionKey  当前 Selectionkey
     * @return SocketChannel 对象
     * @throws IOException  IO 异常
     */
    public DatagramChannel getDatagramChannel(SelectionKey selectionKey)
            throws IOException {
        DatagramChannel datagramChannel = null;
        // 取得通道
        Object unknowChannel = selectionKey.channel();

        if (unknowChannel instanceof DatagramChannel) {
            datagramChannel = (DatagramChannel)unknowChannel;
        }
        return datagramChannel;
    }


    public void release(){
        if(readTempBuffer!=null){
            TByteBuffer.release(readTempBuffer);
        }
    }
}
