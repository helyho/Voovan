package org.voovan.network.udp;

import org.voovan.network.EventTrigger;
import org.voovan.network.HeartBeat;
import org.voovan.network.MessageLoader;
import org.voovan.network.SocketContext;
import org.voovan.tools.TByteBuffer;
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


    private Selector selector;
    private SocketContext socketContext;
    private UdpSession session;

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
        }
    }

    /**
     * 所有的事件均在这里触发
     */
    public void eventChose() {
        //读取用的缓冲区
        ByteBuffer readTempBuffer = TByteBuffer.allocateDirect(socketContext.getBufferSize());

        if (socketContext instanceof UdpSocket) {
            // 连接完成onConnect事件触发
            EventTrigger.fireConnectThread(session);
        }

        // 事件循环
        try {
            while (socketContext != null && socketContext.isOpen()) {
                if (selector.select(1000) > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> selectionKeyIterator = selectionKeys
                            .iterator();
                    while (selectionKeyIterator.hasNext()) {
                        SelectionKey selectionKey = selectionKeyIterator.next();
                        if (selectionKey.isValid()) {
                            // 获取 socket 通道
                            DatagramChannel datagramChannel = getDatagramChannel(selectionKey);
                            if (datagramChannel.isOpen() && selectionKey.isValid()) {
                                // 事件分发,包含时间 onRead onAccept

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
                                            clientUdpSocket = new UdpSocket(socketContext,(InetSocketAddress)address);
                                            session = clientUdpSocket.getSession();
                                            //触发连接时间, 关闭事件在触发 onSent 之后触发
                                            EventTrigger.fireConnectThread(session);
                                        }

                                        //判断连接是否关闭
                                        if (MessageLoader.isStreamEnd(readTempBuffer, readSize) && session.isConnected()) {

                                            session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
                                            //如果 Socket 流达到结尾,则关闭连接
                                            while(session.isConnected()) {
                                                if (session.getByteBufferChannel().size() == 0) {
                                                    session.close();
                                                }
                                            }
                                            break;
                                        } else if (readSize > 0) {
                                            readTempBuffer.flip();

                                            if(session.getHeartBeat()!=null) {
                                                session.getMessageLoader().pause();
                                            }

                                            session.getByteBufferChannel().writeEnd(readTempBuffer);

                                            //检查心跳
                                            HeartBeat.interceptHeartBeat(session,  session.getByteBufferChannel());

                                            if(session.getHeartBeat()!=null) {
                                                session.getMessageLoader().unpause();
                                            }

                                            readTempBuffer.clear();
                                            // 触发 onRead 事件,如果正在处理 onRecive 事件则本次事件触发忽略
                                            EventTrigger.fireReceiveThread(session);
                                        }

                                        readTempBuffer.clear();
                                        break;
                                    } default: {
                                        Logger.debug("Nothing to do ,SelectionKey is:"
                                                + selectionKey.readyOps());
                                    }
                                }
                                selectionKeyIterator.remove();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            // 触发 onException 事件
            EventTrigger.fireExceptionThread(session, e);
        }finally{
            // 触发连接断开事件
            if(session!=null) {
//                EventTrigger.fireDisconnectThread(session);
                TByteBuffer.release(readTempBuffer);
            }
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
}
