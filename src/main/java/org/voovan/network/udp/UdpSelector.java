package org.voovan.network.udp;

import org.voovan.network.EventTrigger;
import org.voovan.network.MessageLoader;
import org.voovan.network.SocketContext;
import org.voovan.tools.TObject;
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
        ByteBuffer readTempBuffer = ByteBuffer.allocate(1024);

        if (socketContext instanceof UdpSocket) {
            // 连接完成onConnect事件触发
            EventTrigger.fireConnectThread(session);
        }

        // 事件循环
        try {
            while (socketContext != null && socketContext.isConnect()) {
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
                                        UdpSession clientSession = session;

                                        if(datagramChannel.isConnected()) {
                                            readSize = datagramChannel.read(readTempBuffer);
                                        }else{
                                            SocketAddress address = datagramChannel.receive(readTempBuffer);
                                            readSize = readTempBuffer.position();
                                            clientUdpSocket = new UdpSocket(socketContext,(InetSocketAddress)address);
                                            clientSession = clientUdpSocket.getSession();
                                        }

                                            //判断连接是否关闭
                                        if (MessageLoader.isRemoteClosed(readSize, readTempBuffer) && clientSession.isConnect()) {
                                            clientSession.close();
                                            break;
                                        } else if (readSize > 0) {
                                            readTempBuffer.flip();
                                            clientSession.getByteBufferChannel().write(readTempBuffer);
                                            readTempBuffer.clear();
                                        } else if (readSize == -1) {
                                            clientSession.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
                                            break;
                                        }

                                        // 触发 onRead 事件,如果正在处理 onRead 事件则本次事件触发忽略
                                        EventTrigger.fireReceiveThread(clientSession);

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
            datagramChannel = TObject.cast( unknowChannel );
        }
        return datagramChannel;
    }
}
