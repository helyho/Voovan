package org.voovan.network.nio;

import org.voovan.Global;
import org.voovan.network.*;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.TPerformance;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
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
public class NioSelector {
	public static ArrayBlockingQueue<NioSelector> SELECTORS = new ArrayBlockingQueue<NioSelector>(200000);

	public static int IO_THREAD_COUNT = TPerformance.getProcessorCount()/2;
	static {
		for(int i=0;i<IO_THREAD_COUNT;i++) {
			Global.getThreadPool().execute(() -> {
				while (true) {
					try {
						NioSelector nioSelector = SELECTORS.take();
						if (nioSelector != null && nioSelector.socketContext.isConnected()) {
							try {
								nioSelector.eventChose();
							} catch (Throwable e) {
								e.printStackTrace();
							}
							SELECTORS.offer(nioSelector);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}
			});
		}
	}

	public static void register(NioSelector selector){
		SELECTORS.add(selector);
	}

	public static void unregister(NioSelector selector){
		SELECTORS.remove(selector);
	}

	private Selector selector;
	private SocketContext socketContext;
	private ByteBufferChannel netByteBufferChannel;
	private ByteBufferChannel appByteBufferChannel;
	private ByteBuffer readTempBuffer;

	private NioSession session;
	private SelectorKeySet selectionKeys = new SelectorKeySet(1024);

	/**
	 * 事件监听器构造
	 * @param selector   对象Selector
	 * @param socketContext socketContext 对象
	 */
	public NioSelector(Selector selector, SocketContext socketContext) {
		this.selector = selector;
		this.socketContext = socketContext;

		//读取用的缓冲区
		readTempBuffer = TByteBuffer.allocateDirect(socketContext.getReadBufferSize());

		if (socketContext instanceof NioSocket){
			this.session = ((NioSocket)socketContext).getSession();
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
	private void eventChose() {
		// 事件循环
		try {
			if (socketContext != null && socketContext.isConnected()) {
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
							SocketChannel socketChannel = getSocketChannel(selectionKey);
							if (socketChannel.isOpen() && selectionKey.isValid()) {
								// 事件分发,包含时间 onRead onAccept
								try {
									// Server接受连接
									if(selectionKey.isAcceptable()){
										accept(socketChannel);
									} else {
										// 有数据读取
										if (selectionKey.isReadable()) {
											read(socketChannel);
										}
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

					selectionKeys.reset();
				}
			}
		} catch (IOException e) {
			if((e instanceof AsynchronousCloseException) ||
					(e instanceof ClosedChannelException)){
				return;
			}

			//兼容 windows 的 "java.io.IOException: 指定的网络名不再可用" 错误
			if(e.getStackTrace()[0].getClassName().contains("sun.nio.ch")){
				return;
			}

			if(e instanceof Exception){
				//触发 onException 事件
				EventTrigger.fireExceptionThread(session, e);
			}
		}
	}

	public void accept(SocketChannel socketChannel){
		NioServerSocket serverSocket = (NioServerSocket) socketContext;
		NioSocket socket = new NioSocket(serverSocket, socketChannel);
		EventTrigger.fireAcceptThread(socket.getSession());
	}


	public void read(SocketChannel socketChannel) throws IOException {
		int length = socketChannel.read(readTempBuffer);

		// 如果对端连接关闭,或者 session 关闭,则直接调用 session 的关闭
		if (MessageLoader.isStreamEnd(readTempBuffer, length) || !session.isConnected()) {
			session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
			session.close();
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
					EventTrigger.fireReceiveThread(session);
				}

				// 接收完成后重置buffer对象
				readTempBuffer.clear();
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
	public SocketChannel getSocketChannel(SelectionKey selectionKey)
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
