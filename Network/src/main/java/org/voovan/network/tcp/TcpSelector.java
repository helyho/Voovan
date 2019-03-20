package org.voovan.network.tcp;

import org.voovan.network.*;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.TimeoutException;

/**
 * 事件监听器
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TcpSelector extends IoSelector<SocketChannel, TcpSession> {

	/**
	 * 事件监听器构造
	 * @param selector   对象Selector
	 * @param socketContext socketContext 对象
	 */
	public TcpSelector(Selector selector, SocketContext socketContext) {
		this.selector = selector;
		this.socketContext = socketContext;

		//读取用的缓冲区
		readTempBuffer = TByteBuffer.allocateDirect(socketContext.getReadBufferSize());

		if (socketContext instanceof TcpSocket){
			this.session = ((TcpSocket)socketContext).getSession();
			this.appByteBufferChannel = session.getReadByteBufferChannel();
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
		// 事件循环
		EventRunner eventRunner = socketContext.getEventRunner();

		try {
			if (socketContext != null && socketContext.isConnected()) {
				int readyChannelCount = selector.selectNow();

				if (readyChannelCount==0) {
					if(eventRunner.getEventQueue().isEmpty()) {
						readyChannelCount = selector.select(1);
					} else {
						return;
					}
				}

				if (readyChannelCount>0) {
					SelectionKeySet selectionKeys = (SelectionKeySet) selector.selectedKeys();

					for (int i=0;i<selectionKeys.size(); i++) {
						SelectionKey selectionKey = selectionKeys.getSelectionKeys()[i];

						if (selectionKey.isValid()) {
							// 获取 socket 通道
							SocketChannel socketChannel = getChannel(selectionKey);
							if (socketChannel.isOpen() && selectionKey.isValid()) {
								// 事件分发,包含时间 onRead onAccept
								try {
									// Server接受连接
									if((selectionKey.readyOps() & SelectionKey.OP_ACCEPT) != 0){
										accept(socketChannel);
									}

									// 有数据读取
									if ((selectionKey.readyOps() & SelectionKey.OP_READ) != 0) {
										session.setSelectionKey(selectionKey);
										readFromChannel();
									}
								} catch (Exception e) {
									//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
									if(e.getStackTrace()[0].getClassName().contains("sun.tcp.ch")){
										return;
									} else if(e instanceof Exception){
										//触发 onException 事件
										EventTrigger.fireException(session, e);
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
		} catch (IOException e) {
			if((e instanceof AsynchronousCloseException) ||
					(e instanceof ClosedChannelException)){
				return;
			}

			//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
			if(e.getStackTrace()[0].getClassName().contains("sun.tcp.ch")){
				return;
			}

			if(e instanceof Exception){
				//触发 onException 事件
				EventTrigger.fireException(session, e);
			}
		} finally {
			if(socketContext.isConnected()) {
				eventRunner.addEvent(() -> {
					if(socketContext.isConnected()) {
						eventChose();
					}
				});
			}
		}
	}

	public void accept(SocketChannel socketChannel){
		TcpServerSocket serverSocket = (TcpServerSocket) socketContext;
		TcpSocket socket = new TcpSocket(serverSocket, socketChannel);
		EventTrigger.fireAccept(socket.getSession());
	}


	public int readFromChannel() throws IOException {
		SocketChannel socketChannel = (SocketChannel) socketContext.socketChannel();
		int length = socketChannel.read(readTempBuffer);

		// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
		if (MessageLoader.isStreamEnd(readTempBuffer, length) || !session.isConnected()) {
			session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
			session.close();
			return -1;
		} else {
			if (netByteBufferChannel == null && session.getSSLParser() != null) {
				netByteBufferChannel = new ByteBufferChannel(session.socketContext().getReadBufferSize());
			}

			readTempBuffer.flip();

			if (length > 0) {

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
		}

		return length;
	}

	public int writeToChannel(ByteBuffer buffer) throws IOException {
		int totalSendByte = 0;
		long start = System.currentTimeMillis();
		if (socketContext.isConnected() && buffer != null) {
			//循环发送直到全部内容发送完毕
			while(socketContext.isConnected() && buffer.remaining()!=0){
				int sendSize = session.socketChannel().write(buffer);
				if(sendSize == 0 ){
					TEnv.sleep(1);
					if(System.currentTimeMillis() - start >= socketContext.getSendTimeout()) {
						Logger.error("NioSession send0 timeout", new TimeoutException());
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
	}

	/**
	 * 获取 socket 通道
	 *
	 * @param selectionKey  当前 Selectionkey
	 * @return SocketChannel 对象
	 * @throws IOException  IO 异常
	 */
	public SocketChannel getChannel(SelectionKey selectionKey)
			throws IOException {
		SocketChannel socketChannel = null;
		// 取得通道
		Object unknowChannel = selectionKey.channel();
		//  根据通道的类来判断类型是 ServerSocketChannel 还是 SocketChannel
		if (unknowChannel instanceof ServerSocketChannel) {
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel)unknowChannel;
			socketChannel = serverSocketChannel.accept();
		} else if (unknowChannel instanceof SocketChannel) {
			socketChannel = (SocketChannel)unknowChannel;
		}
		return socketChannel;
	}

	public void release(){
		if(readTempBuffer!=null){
			TByteBuffer.release(readTempBuffer);
		}

		if(netByteBufferChannel!=null) {
			netByteBufferChannel.release();
		}
	}
}
