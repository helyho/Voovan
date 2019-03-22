//package org.voovan.network.udp;
//
//import org.voovan.network.*;
//import org.voovan.network.tcp.SelectionKeySet;
//import org.voovan.tools.TByteBuffer;
//import org.voovan.tools.TEnv;
//import org.voovan.tools.log.Logger;
//import org.voovan.tools.reflect.TReflect;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.net.SocketAddress;
//import java.nio.ByteBuffer;
//import java.nio.channels.DatagramChannel;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.Selector;
//import java.util.concurrent.TimeoutException;
//
///**
// * UDP事件监听器
// *
// * @author helyho
// *
// * Voovan Framework.
// * WebSite: https://github.com/helyho/Voovan
// * Licence: Apache v2 License
// */
//public class UdpSelector extends IoSelector<DatagramChannel, UdpSession>{
//
//    /**
//     * 事件监听器构造
//     * @param selector   对象Selector
//     * @param socketContext socketContext 对象
//     */
//    public UdpSelector(Selector selector, SocketContext socketContext) {
//        this.selector = selector;
//        this.socketContext = socketContext;
//
//	    readTempBuffer = TByteBuffer.allocateDirect(socketContext.getReadBufferSize());
//
//        if (socketContext instanceof UdpSocket){
//            session = ((UdpSocket)socketContext).getSession();
//            appByteBufferChannel = session.getReadByteBufferChannel();
//        }
//
//
//	    try {
//		    TReflect.setFieldValue(selector, NioUtil.selectedKeysField, selectionKeys);
//		    TReflect.setFieldValue(selector, NioUtil.publicSelectedKeysField, selectionKeys);
//	    } catch (ReflectiveOperationException e) {
//		    e.printStackTrace();
//	    }
//    }
//
//    /**
//     * 所有的事件均在这里触发
//     */
//    public void eventChoose() {
//	    // 事件循环
//	    EventRunner eventRunner = socketContext.getEventRunner();
//
//        // 事件循环
//        try {
//            if (socketContext != null && socketContext.isOpen()) {
//	            int readyChannelCount = selector.selectNow();
//
//	            if (readyChannelCount==0) {
//		            if(eventRunner.getEventQueue().isEmpty()) {
//			            readyChannelCount = selector.select(1);
//		            } else {
//			            return;
//		            }
//	            }
//
//	            if (readyChannelCount>0) {
//	                SelectionKeySet selectionKeys = (SelectionKeySet) selector.selectedKeys();
//
//	                for (int i=0;i<selectionKeys.size(); i++) {
//		                SelectionKey selectionKey = selectionKeys.getSelectionKeys()[i];
//
//                        if (selectionKey.isValid()) {
//                            // 获取 socket 通道
//                            DatagramChannel datagramChannel = getChannel(selectionKey);
//                            if (datagramChannel.isOpen() && selectionKey.isValid()) {
//
//								// 事件分发,包含时间 onRead onAccept
//								try {
//									if (selectionKey.isReadable()) {
//										if(session!=null) {
//											session.setSelectionKey(selectionKey);
//										}
//										readFromChannel();
//									} else {
//										Logger.fremawork("Nothing to do ,SelectionKey is:" + selectionKey.readyOps());
//									}
//								} catch (Exception e) {
//									if(e instanceof IOException){
//										session.close();
//									}
//									//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
//									else if(e.getStackTrace()[0].getClassName().contains("sun.tcp.ch")){
//										return;
//									} else if(e instanceof Exception){
//										//触发 onException 事件
//										EventTrigger.fireException(session, e);
//									}
//								}
//                            }
//                        }
//                    }
//                }
//            }
//        } catch (IOException e) {
//            // 触发 onException 事件
//            EventTrigger.fireException(session, e);
//        } finally {
//	        if(socketContext.isConnected()) {
//		        eventRunner.addEvent(() -> {
//			        if(socketContext.isConnected()) {
//				        eventChoose();
//			        }
//		        });
//	        }
//        }
//    }
//
//    public int readFromChannel() throws IOException {
//	    DatagramChannel datagramChannel = (DatagramChannel) socketContext.socketChannel();
//
//	    int readSize = - 1;
//	    UdpSocket clientUdpSocket = null;
//
//	    //接受的连接isConnected 是 false
//	    //发起的连接isConnected 是 true
//	    if(datagramChannel.isConnected()) {
//		    readSize = datagramChannel.read(readTempBuffer);
//	    }else{
//		    SocketAddress address = datagramChannel.receive(readTempBuffer);
//		    readSize = readTempBuffer.position();
//		    clientUdpSocket = new UdpSocket(socketContext, datagramChannel, (InetSocketAddress)address);
//		    session = clientUdpSocket.getSession();
//		    appByteBufferChannel = session.getReadByteBufferChannel();
//		    clientUdpSocket.acceptStart();
//	    }
//
//	    //判断连接是否关闭
//	    if (MessageLoader.isStreamEnd(readTempBuffer, readSize) && session.isConnected()) {
//		    session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
//		    session.close();
//		    return -1;
//	    } else if (readSize > 0) {
//		    readTempBuffer.flip();
//
//		    //如果缓冲队列已慢, 则等待可用, 超时时间为读超时
//		    try {
//			    TEnv.wait(session.socketContext().getReadTimeout(), () -> appByteBufferChannel.size() + readTempBuffer.limit() >= appByteBufferChannel.getMaxSize());
//		    } catch (TimeoutException e) {
//			    Logger.error("Session.byteByteBuffer is not enough:", e);
//		    }
//
//		    appByteBufferChannel.writeEnd(readTempBuffer);
//
//		    if(appByteBufferChannel.size() > 0) {
//			    // 触发 onReceive 事件
//			    EventTrigger.fireReceive(session);
//		    }
//
//		    readTempBuffer.clear();
//	    }
//
//	    return readSize;
//    }
//
//
//	public int writeToChannel(ByteBuffer buffer) throws IOException {
//		int totalSendByte = 0;
//		long start = System.currentTimeMillis();
//		if (socketContext.isOpen() && buffer != null) {
//			DatagramChannel datagramChannel = (DatagramChannel)socketContext.socketChannel();
//
//			//循环发送直到全部内容发送完毕
//			while(buffer.remaining()!=0){
//				int sendSize = 0;
//				if(datagramChannel.getRemoteAddress()!=null) {
//					sendSize = datagramChannel.write(buffer);
//				} else {
//					sendSize = datagramChannel.send(buffer, session.getInetSocketAddress());
//				}
//				if(sendSize == 0 ){
//					TEnv.sleep(1);
//					if(System.currentTimeMillis() - start >= socketContext.getSendTimeout()){
//						Logger.error("AioSession writeToChannel timeout, Socket will be close");
//						socketContext.close();
//						return -1;
//					}
//				} else {
//					start = System.currentTimeMillis();
//					totalSendByte += sendSize;
//				}
//			}
//		}
//		return totalSendByte;
//	}
//
//    /**
//     * 获取 socket 通道
//     *
//     * @param selectionKey  当前 Selectionkey
//     * @return SocketChannel 对象
//     * @throws IOException  IO 异常
//     */
//    public DatagramChannel getChannel(SelectionKey selectionKey)
//            throws IOException {
//        DatagramChannel datagramChannel = null;
//        // 取得通道
//        Object unknowChannel = selectionKey.channel();
//
//        if (unknowChannel instanceof DatagramChannel) {
//            datagramChannel = (DatagramChannel)unknowChannel;
//        }
//        return datagramChannel;
//    }
//
//
//    public void release(){
//        if(readTempBuffer!=null){
//            TByteBuffer.release(readTempBuffer);
//        }
//    }
//}
