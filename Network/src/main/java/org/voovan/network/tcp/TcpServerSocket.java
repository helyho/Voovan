package org.voovan.network.tcp;

import org.voovan.network.ConnectModel;
import org.voovan.network.ConnectType;
import org.voovan.network.SocketContext;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * NioServerSocket 监听
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TcpServerSocket extends SocketContext<ServerSocketChannel, TcpSession> {

	private SelectorProvider provider;
	private ServerSocketChannel serverSocketChannel;

	//用来阻塞当前Socket
	private Object waitObj = null;

	/**
	 * 构造函数
	 * 		默认不会出发空闲事件, 默认发超时时间: 1s
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @throws IOException	异常
	 */
	public TcpServerSocket(String host, int port, int readTimeout) throws IOException{
		super(host, port, readTimeout);
	}

	/**
	 * 构造函数
	 *      默认发超时时间: 1s
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param idleInterval	空闲事件触发时间, 单位: 秒
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @throws IOException	异常
	 */
	public TcpServerSocket(String host, int port, int readTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, idleInterval);
	}

	/**
	 * 构造函数
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param idleInterval	空闲事件触发时间, 单位: 秒
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @param sendTimeout 发超时时间, 单位: 毫秒
	 * @throws IOException	异常
	 */
	public TcpServerSocket(String host, int port, int readTimeout, int sendTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, sendTimeout, idleInterval);
	}

	/**
	 * 初始化函数
	 * @throws IOException
	 */
	private void init() throws IOException{
		this.provider = SelectorProvider.provider();
		this.serverSocketChannel = provider.openServerSocketChannel();
		this.serverSocketChannel.socket().setSoTimeout(this.readTimeout);
		this.serverSocketChannel.configureBlocking(false);
		this.connectModel = ConnectModel.LISTENER;
		this.connectType = ConnectType.TCP;
	}

	@Override
	public void setIdleInterval(int idleInterval) {
		this.idleInterval = idleInterval;
	}

	/**
	 * 设置 Socket 的 Option 选项
	 *
	 * @param name    SocketOption类型的枚举, 参照:ServerSocketChannel.setOption的说明
	 * @param value  SocketOption参数
	 * @throws IOException IO异常
	 */
	public <T> void setOption(SocketOption<T> name, T value) throws IOException {
		serverSocketChannel.setOption(name, value);
	}

	/**
	 * 获取 SocketChannel 对象
	 * @return SocketChannel 对象
	 */
	public ServerSocketChannel socketChannel(){
		return this.serverSocketChannel;
	}

	@Override
	public TcpSession getSession() {
		return null;
	}

	/**
	 * 启动监听
	 * 		阻赛方法
	 * @throws IOException  IO 异常
	 */
	@Override
	public void start() throws IOException {

		syncStart();

		waitObj = new Object();
		synchronized (waitObj){
			try {
				waitObj.wait();
			} catch (InterruptedException e) {
				Logger.error(e);
			}
		}
	}

	/**
	 * 启动同步监听
	 * 		非阻赛方法
	 * @throws IOException  IO 异常
	 */
	@Override
	public void syncStart() throws IOException {
		init();
		serverSocketChannel.bind(new InetSocketAddress(host, port), 512);
		bindToSocketSelector(SelectionKey.OP_ACCEPT);
	}

	@Override
	protected void acceptStart() throws IOException {
		throw new RuntimeException("Unsupport method");
	}

	@Override
	public boolean isOpen() {
		if(serverSocketChannel!=null){
			return serverSocketChannel.isOpen();
		}

		return false;
	}

	@Override
	public boolean isConnected() {
		if(serverSocketChannel!=null){
			return serverSocketChannel.isOpen();
		} else {
			return false;
		}
	}

	/**
	 * 重启 ServerSocket
	 * @return true:成功, false: 失败
	 */
	public boolean restart(){
		try {
			if(serverSocketChannel!=null && serverSocketChannel.isOpen()){
				serverSocketChannel.close();
			}
			int acceptSize = this.getAcceptEventRunnerGroup().getThreadPool().getPoolSize();
			int ioSize = this.getIoEventRunnerGroup().getThreadPool().getPoolSize();

			this.getAcceptEventRunnerGroup().close();
			this.getIoEventRunnerGroup().close();

			setAcceptEventRunnerGroup(SocketContext.createEventRunnerGroup("Web", acceptSize+1, true));
			setIoEventRunnerGroup(SocketContext.createEventRunnerGroup("Web", ioSize+1, false));

			TEnv.sleep(1000);

			syncStart();

			return true;
		} catch(IOException e){
			Logger.error("TcpServerSocket.close failed", e);
			return false;
		}
	}

	@Override
	public boolean close() {
		try {
			if(serverSocketChannel!=null && serverSocketChannel.isOpen()){
				serverSocketChannel.close();
				return true;
			}
		} catch(IOException e){
			Logger.error("TcpServerSocket.close failed", e);
		} finally {
			if(waitObj!=null) {
				synchronized (waitObj) {
					waitObj.notify();
				}
			}
		}

		return false;
	}
}
