package org.voovan.network.nio;

import org.voovan.Global;
import org.voovan.network.*;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

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

	public static ConcurrentLinkedDeque<NioSelector> SELECTORS = new ConcurrentLinkedDeque<NioSelector>();
	static {
		Global.getThreadPool().execute(()->{
			while(true){
				NioSelector nioSelector = SELECTORS.poll();
				if(nioSelector!=null && nioSelector.socketContext.isConnected()) {
					nioSelector.eventChose();
					SELECTORS.offer(nioSelector);
				}
			}
		});
	}

	private Selector selector;
	private SocketContext socketContext;
	private ByteBufferChannel netByteBufferChannel;
	private ByteBufferChannel sessionByteBufferChannel;
	private ByteBufferChannel tmpByteBufferChannel;
	private ByteBuffer readTempBuffer;
	private int waitCount = 0;

	private NioSession session;

	/**
	 * 事件监听器构造
	 * @param selector   对象Selector
	 * @param socketContext socketContext 对象
	 */
	public NioSelector(Selector selector, SocketContext socketContext) {
		this.selector = selector;
		this.socketContext = socketContext;

		//读取用的缓冲区
		readTempBuffer = TByteBuffer.allocateDirect(socketContext.getBufferSize());

		if (socketContext instanceof NioSocket){
			this.session = ((NioSocket)socketContext).getSession();
			this.sessionByteBufferChannel = session.getByteBufferChannel();
			this.tmpByteBufferChannel = new ByteBufferChannel();

			if(netByteBufferChannel== null && session.getSSLParser()!=null){
				netByteBufferChannel = new ByteBufferChannel(session.socketContext().getBufferSize());

			}
		}
	}

	public static void register(NioSelector selector){
		SELECTORS.add(selector);
	}

	public static void unregister(NioSelector selector){
		SELECTORS.remove(selector);
	}

	/**
	 * 所有的事件均在这里触发
	 */
	private void eventChose() {
		// 事件循环
		try {
			if (socketContext != null && socketContext.isConnected()) {
				if (selector.select(1) > 0) {
					Set<SelectionKey> selectionKeys = selector.selectedKeys();
					Iterator<SelectionKey> selectionKeyIterator = selectionKeys
							.iterator();
					while (selectionKeyIterator.hasNext()) {
						waitCount = 0;
						SelectionKey selectionKey = selectionKeyIterator.next();
						if (selectionKey.isValid()) {
							// 获取 socket 通道
							SocketChannel socketChannel = getSocketChannel(selectionKey);
							if (socketChannel.isOpen() && selectionKey.isValid()) {
								// 事件分发,包含时间 onRead onAccept

								switch (selectionKey.readyOps()) {
									// Server接受连接
									case SelectionKey.OP_ACCEPT: {
										NioServerSocket serverSocket = (NioServerSocket)socketContext;
										NioSocket socket = new NioSocket(serverSocket,socketChannel);
										EventTrigger.fireAccept(socket.getSession());
										break;
									}
									// 有数据读取
									case SelectionKey.OP_READ: {
										int readSize = socketChannel.read(readTempBuffer);

										//判断连接是否关闭
										if(MessageLoader.isStreamEnd(readTempBuffer, readSize) && session.isConnected()){

											session.getMessageLoader().setStopType(MessageLoader.StopType.STREAM_END);
											session.close();
											break;
										}else if(readSize>0){
											readTempBuffer.flip();

											tmpByteBufferChannel.clear();

											//接收SSL数据, SSL握手完成后解包
											if(session.getSSLParser()!=null && SSLParser.isHandShakeDone(session)){
												//一次接受并完成 SSL 解码后, 常常有剩余无法解码数据, 所以用 netByteBufferChannel 这个通道进行保存
												netByteBufferChannel.writeEnd(readTempBuffer);
												session.getSSLParser().unWarpByteBufferChannel(session, netByteBufferChannel, tmpByteBufferChannel);
											}

											//如果在没有 SSL 支持 和 握手没有完成的情况下,直接写入
											if(session.getSSLParser()==null || !SSLParser.isHandShakeDone(session)){
												tmpByteBufferChannel.writeEnd(readTempBuffer);
											}

											//检查心跳
											if(SSLParser.isHandShakeDone(session)) {
												HeartBeat.interceptHeartBeat(session, tmpByteBufferChannel);
											}

											if(tmpByteBufferChannel.size() > 0) {
												sessionByteBufferChannel.writeEnd(tmpByteBufferChannel.getByteBuffer());
												tmpByteBufferChannel.compact();

												// 触发 onReceive 事件
												EventTrigger.fireReceiveThread(session);
											}
										}

										// 接收完成后重置buffer对象
										readTempBuffer.clear();

										break;
									}
									default: {
										Logger.fremawork("Nothing to do ,SelectionKey is:"
												+ selectionKey.readyOps());
									}
								}
								selectionKeyIterator.remove();
							}
						}
					}
				} else {
					//ReadTimeout 控制
					waitCount++;
					if(socketContext instanceof NioSocket && waitCount >= socketContext.getReadTimeout()) {
						session.close();
					}
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

		if(tmpByteBufferChannel!=null) {
			tmpByteBufferChannel.release();
		}
	}
}
