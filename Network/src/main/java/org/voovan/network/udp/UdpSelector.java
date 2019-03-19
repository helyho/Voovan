package org.voovan.network.udp;

import org.voovan.Global;
import org.voovan.network.*;
import org.voovan.network.nio.NioSelector;
import org.voovan.network.nio.NioUtil;
import org.voovan.network.nio.SelectorKeySet;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.TPerformance;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
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

    public static ArrayBlockingQueue<UdpSelector> SELECTORS = new ArrayBlockingQueue<UdpSelector>(200000);


	public static int IO_THREAD_COUNT = TPerformance.getProcessorCount()/2;
	static {
		for(int i=0;i<IO_THREAD_COUNT;i++) {
			Global.getThreadPool().execute(() -> {
				while (true) {
					try {
						UdpSelector udpSelector = SELECTORS.take();
						if (udpSelector != null && udpSelector.socketContext.isOpen()) {
							try {
								udpSelector.eventChose();
							} catch (Throwable e) {
								e.printStackTrace();
							}
							SELECTORS.offer(udpSelector);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
			});
		}
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
	private ByteBuffer readTempBuffer;

	private UdpSession session;
	private SelectorKeySet selectionKeys = new SelectorKeySet(1024);

    /**
     * 事件监听器构造
     * @param selector   对象Selector
     * @param socketContext socketContext 对象
     */
    public UdpSelector(Selector selector, SocketContext socketContext) {
        this.selector = selector;
        this.socketContext = socketContext;

	    readTempBuffer = TByteBuffer.allocateDirect(socketContext.getReadBufferSize());

        if (socketContext instanceof UdpSocket){
            session = ((UdpSocket)socketContext).getSession();
            appByteBufferChannel = session.getReadByteBufferChannel();
        }


	    try {
		    TReflect.setFieldValue(selector, NioUtil.selectedKeysField, selectionKeys);
		    TReflect.setFieldValue(selector, NioUtil.publicSelectedKeysField, selectionKeys);
	    } catch (ReflectiveOperationException e) {
		    e.printStackTrace();
	    }
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
	            int readyChannelCount = selector.selectNow();

	            if (readyChannelCount==0) {
		            readyChannelCount = selector.select(1);
	            }

	            if (readyChannelCount>0) {
	                SelectorKeySet selectionKeys = (SelectorKeySet) selector.selectedKeys();

	                for (int i=0;i<selectionKeys.size(); i++) {
		                SelectionKey selectionKey = selectionKeys.getSelectionKeys()[i];

                        if (selectionKey.isValid()) {
                            // 获取 socket 通道
                            DatagramChannel datagramChannel = getDatagramChannel(selectionKey);
                            if (datagramChannel.isOpen() && selectionKey.isValid()) {

								// 事件分发,包含时间 onRead onAccept
								try {
									if (selectionKey.isReadable()) {
										read(datagramChannel);
									} else {
										Logger.fremawork("Nothing to do ,SelectionKey is:" + selectionKey.readyOps());
									}
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

    public void read( DatagramChannel datagramChannel) throws IOException {

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
		    EventTrigger.fireConnectThread(session);
	    }

	    //判断连接是否关闭
	    if (MessageLoader.isStreamEnd(readTempBuffer, readSize) && session.isConnected()) {
		    session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
		    session.close();
		    return;
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
