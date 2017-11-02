package org.voovan.network.nio;

import org.voovan.Global;
import org.voovan.network.SocketContext;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
public class NioServerSocket extends SocketContext {

	private SelectorProvider provider;
	private Selector selector;
	private ServerSocketChannel serverSocketChannel;

	/**
	 * 构造函数
	 * 		默认不会出发空闲事件, 默认发超时时间: 1s
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param readTimeout   超时时间, 单位: 毫秒
	 * @throws IOException	异常
	 */
	public NioServerSocket(String host,int port,int readTimeout) throws IOException{
		super(host, port, readTimeout);
		init();
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
	public NioServerSocket(String host,int port,int readTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, idleInterval);
		init();
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
	public NioServerSocket(String host,int port,int readTimeout, int sendTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, sendTimeout, idleInterval);
		init();
	}

	/**
	 * 初始化函数
	 * @throws IOException
	 */
	private void init() throws IOException{
		provider = SelectorProvider.provider();
		serverSocketChannel = provider.openServerSocketChannel();
		serverSocketChannel.socket().setSoTimeout(this.readTimeout);
		serverSocketChannel.configureBlocking(false);
		selector = provider.openSelector();
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		serverSocketChannel.bind(new InetSocketAddress(host, port), 1000);
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

	/**
	 * 启动监听
	 * 		阻赛方法
	 * @throws IOException  IO 异常
	 */
	@Override
	public void start() throws IOException {
		NioSelector eventListener = new NioSelector(selector,this);
		eventListener.eventChose();
	}

	/**
	 * 启动同步监听
	 * 		非阻赛方法
	 * @throws IOException  IO 异常
	 */
	@Override
	public void syncStart() throws IOException {
		Global.getThreadPool().execute(new Runnable(){
			public void run() {
				try {
					start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
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
		}else{
			return false;
		}
	}

	@Override
	public boolean close() {
		if(serverSocketChannel!=null && serverSocketChannel.isOpen()){
			try{
				serverSocketChannel.close();
				return true;
			} catch(IOException e){
				Logger.error("SocketChannel close failed",e);
				return false;
			}
		}else{
			return true;
		}
	}
}
