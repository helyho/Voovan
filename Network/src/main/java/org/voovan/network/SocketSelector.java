package org.voovan.network;

import org.voovan.network.tcp.TcpServerSocket;
import org.voovan.network.udp.UdpServerSocket;
import org.voovan.network.udp.UdpSession;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.TimeoutException;

/**
 * 选择器抽象类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SocketSelector implements Closeable {
	private  EventRunner eventRunner;

	protected Selector selector;
	protected ByteBufferChannel netByteBufferChannel;
	protected ByteBuffer readTempBuffer;

	protected SelectionKeySet selectionKeys = new SelectionKeySet(1024);


	public SocketSelector(EventRunner eventRunner) throws IOException {
		this.selector = SelectorProvider.provider().openSelector();
		this.eventRunner = eventRunner;

		readTempBuffer = TByteBuffer.allocateDirect();

		try {
			TReflect.setFieldValue(selector, NioUtil.selectedKeysField, selectionKeys);
			TReflect.setFieldValue(selector, NioUtil.publicSelectedKeysField, selectionKeys);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		}

		eventChose();
	}

	public EventRunner getEventRunner() {
		return eventRunner;
	}


	public boolean register(SocketContext socketContext, int ops){
		try {
			SelectionKey selectionKey = socketContext.socketChannel().register(selector, ops, socketContext);
			if(socketContext.getSession()!=null) {
				socketContext.getSession().setSelectionKey(selectionKey);
				socketContext.getSession().setSocketSelector(this);
			}

			return true;
		} catch (ClosedChannelException e) {
			Logger.error("Register " + socketContext + " to selector error");
			return false;
		}
	}

	public void unRegister(SocketContext socketContext) {
		socketContext.getSession().getSelectionKey().attach(null);
		socketContext.getSession().getSelectionKey().cancel();
	}

	public void eventChose() {
		// 事件循环
		try {
			if (selector != null && selector.isOpen()) {
				int readyChannelCount = selector.selectNow();

				if (readyChannelCount==0 && eventRunner.getEventQueue().isEmpty()) {
					long startNano = System.nanoTime();
					readyChannelCount = selector.select(50);
					System.out.println("s-" + (System.nanoTime() - startNano));
				}

				if (readyChannelCount>0) {
					SelectionKeySet selectionKeys = (SelectionKeySet) selector.selectedKeys();

					for (int i=0;i<selectionKeys.size(); i++) {
						SelectionKey selectionKey = selectionKeys.getSelectionKeys()[i];

						if (selectionKey.isValid()) {
							// 获取 socket 通道
							SelectableChannel channel = selectionKey.channel();
							if (channel.isOpen() && selectionKey.isValid()) {
								// 事件分发,包含时间 onRead onAccept
								// Server接受连接
								if((selectionKey.readyOps() & SelectionKey.OP_ACCEPT) != 0) {
									SocketChannel socketChannel = ((ServerSocketChannel) channel).accept();
									tcpAccept((TcpServerSocket) selectionKey.attachment(), socketChannel);
								}

								// 有数据读取
								if ((selectionKey.readyOps() & SelectionKey.OP_READ) != 0) {
									if(channel instanceof SocketChannel){
										tcpReadFromChannel((TcpSocket) selectionKey.attachment(), (SocketChannel)channel);
									} else if(channel instanceof DatagramChannel) {
										udpReadFromChannel((SocketContext<DatagramChannel, UdpSession>) selectionKey.attachment(), (DatagramChannel) channel);
									}
								}
							}
						} else {
							selectionKey.cancel();
						}
					}

					selectionKeys.reset();
				}
			}
		} catch (IOException e){
			Logger.error("NioSelector error: ", e);
		} finally {
			if(selector.isOpen()) {
				eventRunner.addEvent(() -> {
					eventChose();
				});
			}
		}
	}

	public void close() {
		try {
			TByteBuffer.release(readTempBuffer);
			if(netByteBufferChannel!=null){
				netByteBufferChannel.release();
			}
			selector.close();
		} catch (IOException e) {
			Logger.error("close selector error");
		}
	}

	public int readFromChannel(SocketContext socketContext, SelectableChannel selectableChannel){
		if(selectableChannel instanceof SocketChannel){
			return tcpReadFromChannel((TcpSocket) socketContext, (SocketChannel)selectableChannel);
		} else if(selectableChannel instanceof DatagramChannel){
			return udpReadFromChannel((SocketContext<DatagramChannel, UdpSession>)socketContext, (DatagramChannel) selectableChannel);
		} else {
			return -1;
		}
	}

	public int writeToChannel(SocketContext socketContext, ByteBuffer buffer){
		if(socketContext instanceof TcpSocket){
			return tcpWriteToChannel((TcpSocket) socketContext, buffer);
		} else if(socketContext instanceof UdpSocket){
			return udpWriteToChannel((UdpSocket) socketContext, buffer);
		} else {
			return -1;
		}
	}


	public void tcpAccept(TcpServerSocket socketContext, SocketChannel socketChannel) {
		try {
			TcpSocket socket = new TcpSocket(socketContext, socketChannel);
			EventTrigger.fireAccept(socket.getSession());
		} catch(Exception e){
			dealException(socketContext, e);
		}
	}

	public int tcpReadFromChannel(TcpSocket socketContext, SocketChannel socketChannel) {
		try {
			int readSize = socketChannel.read(readTempBuffer);
			readSize = prepare(socketContext.getSession(), readSize);
			return readSize;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}

	public int tcpWriteToChannel(TcpSocket socketContext, ByteBuffer buffer) {
		try {
			int totalSendByte = 0;
			long start = System.currentTimeMillis();
			if (socketContext.isConnected() && buffer != null) {
				//循环发送直到全部内容发送完毕
				while (socketContext.isConnected() && buffer.remaining() != 0) {
					int sendSize = socketContext.socketChannel().write(buffer);
					if (sendSize == 0) {
						TEnv.sleep(1);
						if (System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
							Logger.error("NioSelector tcpWriteToChannel timeout", new TimeoutException());
							socketContext.close();
							return -1;
						}
					} else {
						start = System.currentTimeMillis();
						totalSendByte += sendSize;
					}
				}
			}
			return totalSendByte;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}

	public UdpSocket udpAccept(UdpServerSocket socketContext, DatagramChannel datagramChannel, SocketAddress address) throws IOException {
		UdpSocket udpSocket = new UdpSocket(socketContext, datagramChannel, (InetSocketAddress) address);
		udpSocket.acceptStart();
		return udpSocket;
	}


	public int udpReadFromChannel(SocketContext<DatagramChannel, UdpSession> socketContext, DatagramChannel datagramChannel) {
		try {
			int readSize = -1;

			//接受的连接isConnected 是 false
			//发起的连接isConnected 是 true
			if (datagramChannel.isConnected()) {
				readSize = datagramChannel.read(readTempBuffer);
			} else {
				socketContext = (UdpSocket) udpAccept((UdpServerSocket) socketContext, datagramChannel, datagramChannel.receive(readTempBuffer));
				UdpSession session = socketContext.getSession();
				readSize = readTempBuffer.position();
			}
			readSize = prepare(socketContext.getSession(), readSize);

			return readSize;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}


	public int udpWriteToChannel(UdpSocket socketContext, ByteBuffer buffer) {
		try {
			DatagramChannel datagramChannel = socketContext.socketChannel();
			UdpSession session = socketContext.getSession();

			int totalSendByte = 0;
			long start = System.currentTimeMillis();
			if (socketContext.isOpen() && buffer != null) {
				//循环发送直到全部内容发送完毕
				while (buffer.remaining() != 0) {
					int sendSize = 0;
					if (datagramChannel.isConnected()) {
						sendSize = datagramChannel.write(buffer);
					} else {
						sendSize = datagramChannel.send(buffer, session.getInetSocketAddress());
					}
					if (sendSize == 0) {
						TEnv.sleep(1);
						if (System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
							Logger.error("NioSelector udpWriteToChannel timeout, Socket will be close");
							socketContext.close();
							return -1;
						}
					} else {
						start = System.currentTimeMillis();
						totalSendByte += sendSize;
					}
				}
			}
			return totalSendByte;
		} catch (Exception e) {
			return dealException(socketContext, e);
		}
	}

	public int prepare(IoSession session, int readSize) throws IOException {
		ByteBufferChannel appByteBufferChannel = session.getReadByteBufferChannel();

		// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
		if (MessageLoader.isStreamEnd(readTempBuffer, readSize) || !session.isConnected()) {
			session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
			session.close();
			return -1;
		} else {
			if (netByteBufferChannel == null && session.getSSLParser() != null) {
				netByteBufferChannel = new ByteBufferChannel(session.socketContext().getReadBufferSize());
			}

			readTempBuffer.flip();

			if (readSize > 0) {

				//如果缓冲队列已慢, 则等待可用, 超时时间为读超时
				try {
					TEnv.wait(session.socketContext().getReadTimeout(), () -> appByteBufferChannel.size() + readTempBuffer.limit() >= appByteBufferChannel.getMaxSize());
				} catch (TimeoutException e) {
					Logger.error("Session.byteByteBuffer is not enough:", e);
				}

				//接收SSL数据, SSL握手完成后解包
				if (session.getSSLParser() != null && SSLParser.isHandShakeDone(session)) {
					//一次接受并完成 SSL 解码后, 常常有剩余无法解码数据, 所以用 netByteBufferChannel 这个通道进行保存
					netByteBufferChannel.writeEnd(readTempBuffer);
					session.getSSLParser().unWarpByteBufferChannel(session, netByteBufferChannel, appByteBufferChannel);
				}

				//如果在没有 SSL 支持 和 握手没有完成的情况下,直接写入
				if (session.getSSLParser() == null || !SSLParser.isHandShakeDone(session)) {
					appByteBufferChannel.writeEnd(readTempBuffer);
				}

				//检查心跳
				if (session.getHeartBeat() != null && SSLParser.isHandShakeDone(session)) {
					//锁住appByteBufferChannel防止异步问题
					appByteBufferChannel.getByteBuffer();
					try {
						HeartBeat.interceptHeartBeat(session, appByteBufferChannel);
					} finally {
						appByteBufferChannel.compact();
					}
				}

				if (appByteBufferChannel.size() > 0 && SSLParser.isHandShakeDone(session)) {
					// 触发 onReceive 事件
					EventTrigger.fireReceive(session);
				}

				// 接收完成后重置buffer对象
				readTempBuffer.clear();
			}

			return readSize;
		}


	}

	public int dealException(SocketContext socketContext, Exception e) {
		if((e instanceof AsynchronousCloseException) ||
				(e instanceof ClosedChannelException)){
			return -1;
		}

		//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
		if(e.getStackTrace()[0].getClassName().contains("sun.tcp.ch")){
			return -1;
		}

		if(e instanceof Exception){
			//触发 onException 事件
			EventTrigger.fireException((IoSession) socketContext.getSession(), e);
		}

		return -1;
	}

}
