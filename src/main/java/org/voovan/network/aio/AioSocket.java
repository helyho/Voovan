package org.voovan.network.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.voovan.network.ConnectModel;
import org.voovan.network.EventTrigger;
import org.voovan.network.SocketContext;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

/**
 * AioSocket 连接
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AioSocket extends SocketContext {

	private AsynchronousSocketChannel	socketChannel;
	private AioSession					session;
	private EventTrigger				eventTrigger;
	private ReadCompletionHandler		readCompletionHandler;
	private ConnectedCompletionHandler	connectedCompletionHandler;

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
	protected AioSocket(SocketContext parentSocketContext, AsynchronousSocketChannel socketChannel) throws IOException {
		this.socketChannel = socketChannel;
		this.copyFrom(parentSocketContext);
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
	protected EventTrigger getEventTrigger() {
		return eventTrigger;
	}

	/**
	 * 捕获 Aio Connect
	 * @throws IOException 
	 */
	protected void catchConnected() throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(this.host, this.port);
		socketChannel.connect(socketAddress, this, connectedCompletionHandler);
		while(true){
			if(socketChannel.getRemoteAddress()!=null){
				break;
			}
		}
	}

	/**
	 * 捕获 Aio Read
	 */
	protected void catchRead(ByteBuffer buffer) {
		if (isConnect()) {
			socketChannel.read(buffer, readTimeout, TimeUnit.MILLISECONDS, buffer, readCompletionHandler);
		}
	}

	public AioSession getSession() {
		return session;
	}

	private void initSSL() throws SSLException{
		if (connectModel == ConnectModel.SERVER && sslManager != null) {
			sslManager.createServerSSLParser(session);
		} else if (connectModel == ConnectModel.CLIENT && sslManager != null) {
			sslManager.createClientSSLParser(session);
		}
	}
	
	@Override
	public void start() throws IOException{
		initSSL();
		
		if (connectModel == ConnectModel.CLIENT) {
			// 捕获 connect 事件
			catchConnected();
		}
		
		//捕获输入事件
		catchRead(ByteBuffer.allocate(1024));
		
		// 触发 connect 事件
		eventTrigger.fireConnectThread();
		
		// 等待ServerSocketChannel关闭,结束进程
		while (isConnect() && (connectModel==ConnectModel.CLIENT || eventTrigger.isShutdown())) {
			TEnv.sleep(500);
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
		if (socketChannel != null) {
			try {

				// 触发 DisConnect 事件
				eventTrigger.fireDisconnect();

				// 检查是否关闭线程池
				// 如果不是ServerSocket下的 Socket 则关闭线程池
				// ServerSocket下的 Socket由 ServerSocket来关闭线程池
				if (connectModel == ConnectModel.CLIENT) {
					eventTrigger.shutdown();
				}

				// 关闭 Socket 连接
				if (socketChannel.isOpen() && (connectModel == ConnectModel.SERVER || eventTrigger.isShutdown())) {
					socketChannel.close();
				}
				return true;
			} catch (IOException e) {
				Logger.error(e);
				return false;
			}
		} else {
			return true;
		}
	}

}
