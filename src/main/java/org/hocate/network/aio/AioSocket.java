package org.hocate.network.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import org.hocate.network.ConnectModel;
import org.hocate.network.EventTrigger;
import org.hocate.network.SocketContext;
import org.hocate.network.aio.completionHandler.ConnectedCompletionHandler;
import org.hocate.network.aio.completionHandler.ReadCompletionHandler;
import org.hocate.tools.TEnv;

/**
 * AioSocket 连接
 * 
 * @author helyho
 *
 */
public class AioSocket extends SocketContext {

	private AsynchronousSocketChannel	socketChannel;
	private AioSession					session;
	private EventTrigger				eventTrigger;
	private ConnectModel				connectModel;
	private ReadCompletionHandler		readCompletionHandler;
	private ConnectedCompletionHandler	connectedCompletionHandler;
	private boolean						isSubSocket	= false;

	/**
	 * 构造函数
	 * 
	 * @param host
	 * @param port
	 * @param readTimeout
	 * @throws IOException
	 */
	public AioSocket(String host, int port, int readTimeout) throws IOException {
		super(host, port, readTimeout);
		this.socketChannel = AsynchronousSocketChannel.open();
		session = new AioSession(this, this.readTimeout);
		eventTrigger = new EventTrigger(session);

		connectedCompletionHandler = new ConnectedCompletionHandler(eventTrigger);
		readCompletionHandler = new ReadCompletionHandler(eventTrigger, session.getByteBufferChannel());
		connectModel = ConnectModel.CLIENT;
	}

	/**
	 * 构造函数
	 * 
	 * @param socketChannel
	 * @param readTimeout
	 * @throws IOException
	 */
	public AioSocket(SocketContext parentSocketContext, AsynchronousSocketChannel socketChannel) throws IOException {
		this.socketChannel = socketChannel;
		this.cloneInit(parentSocketContext);
		isSubSocket = true;
		session = new AioSession(this, this.readTimeout);
		eventTrigger = new EventTrigger(session);

		connectedCompletionHandler = new ConnectedCompletionHandler(eventTrigger);
		readCompletionHandler = new ReadCompletionHandler(eventTrigger, session.getByteBufferChannel());
		connectModel = ConnectModel.SERVER;
	}

	/**
	 * 获取事件触发器
	 * 
	 * @return
	 */
	public EventTrigger getEventTrigger() {
		return eventTrigger;
	}

	/**
	 * 捕获 Aio Connect
	 */
	public void catchConnected() {
		InetSocketAddress socketAddress = new InetSocketAddress(this.host, this.port);
		socketChannel.connect(socketAddress, null, connectedCompletionHandler);
	}

	/**
	 * 捕获 Aio Read
	 */
	public void catchRead(ByteBuffer buffer) {
		if (isConnect()) {
			socketChannel.read(buffer, buffer, readCompletionHandler);
		}
	}

	public AioSession getSession() {
		return session;
	}

	@Override
	public void start() throws Exception {

		if (connectModel == ConnectModel.SERVER && sslManager != null) {
			sslManager.createServerSSLParser(session);
		} else if (connectModel == ConnectModel.CLIENT && sslManager != null) {
			sslManager.createClientSSLParser(session);
		}

		if (connectModel == ConnectModel.SERVER) {
			// 触发 connect 事件
			eventTrigger.fireConnect();
		} else if (connectModel == ConnectModel.CLIENT) {
			// 捕获 connect 事件
			catchConnected();
		}

		if (isConnect()) {
			while (true) {
				try {
					catchRead(ByteBuffer.allocate(1024));
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		// 等待ServerSocketChannel关闭,结束进程
		while (isConnect() && (isSubSocket || eventTrigger.isShutdown())) {
			TEnv.sleep(1);
		}
	}

	/**
	 * 获取 SocketChannel 对象
	 * 
	 * @return
	 */
	public AsynchronousSocketChannel socketChannel() {
		return this.socketChannel;
	}

	@Override
	public boolean isConnect() {
		return socketChannel.isOpen();
	}

	@Override
	public boolean Close() {
		if (socketChannel != null && socketChannel.isOpen()) {
			try {

				// 触发 DisConnect 事件
				eventTrigger.fireDisconnect();

				// 检查是否关闭线程池,如果不是ServerSocket 下的 Socket 则关闭线程池,由 ServerSocket
				// 来关闭
				if (!isSubSocket) {
					eventTrigger.shutdown();
				}

				// 关闭 Socket 连接
				if (socketChannel.isOpen() && (isSubSocket || eventTrigger.isShutdown())) {
					socketChannel.close();
				}
				return true;
			} catch (Throwable e) {
				e.printStackTrace();
				return false;
			}
		} else {
			return true;
		}
	}

}
