package org.voovan.network.aio;

import org.voovan.Global;
import org.voovan.network.EventTrigger;
import org.voovan.network.SocketContext;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketOption;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;

/**
 * AioServerSocket 监听
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AioServerSocket extends SocketContext {

	private AsynchronousServerSocketChannel serverSocketChannel;
	private AcceptCompletionHandler acceptCompletionHandler;


	/**
	 * 构造函数
	 *
	 * @param host    主机地址
	 * @param port    主机端口
	 * @param readTimeout 超时时间, 单位:毫秒
	 * @throws IOException IO 异常
	 */
	public AioServerSocket(String host,int port,int readTimeout) throws IOException{
		super(host, port, readTimeout);
		init();
	}

	/**
	 * 构造函数
	 * @param host    主机地址
	 * @param port    主机端口
	 * @param idleInterval	空闲事件触发时间, 单位: 秒
	 * @param readTimeout 超时时间, 单位:毫秒
	 * @throws IOException IO 异常
	 */
	public AioServerSocket(String host,int port,int readTimeout, int idleInterval) throws IOException{
		super(host, port, readTimeout, idleInterval);
		init();
	}

	private void init() throws IOException {

		serverSocketChannel = AsynchronousServerSocketChannel.open(ASYNCHRONOUS_CHANNEL_GROUP);
		acceptCompletionHandler = new AcceptCompletionHandler();
	}

	@Override
	public void setIdleInterval(int idleInterval) {
		this.idleInterval = idleInterval;
	}

	/**
	 * 设置 Socket 的 Option 选项
	 *
	 * @param name   SocketOption类型的枚举, 参照:AsynchronousServerSocketChannel.setOption的说明
	 * @param value  SocketOption参数
	 * @throws IOException IO异常
	 */
	public <T> void setOption(SocketOption<T> name, T value) throws IOException {
		serverSocketChannel.setOption(name, value);
	}

	/**
	 * 获取 SocketChannel 对象
	 * @return AsynchronousServerSocketChannel对象
	 */
	public AsynchronousServerSocketChannel socketChannel(){
		return this.serverSocketChannel;
	}

	/**
	 * 捕获 Aio Accept 事件
	 */
	protected void catchAccept(){
		serverSocketChannel.accept(this, acceptCompletionHandler);
	}

	/**
	 * 启动监听
	 *   	阻塞方法
	 * @throws IOException  IO 异常
	 */
	@Override
	public void start() throws IOException {
		syncStart();
		//等待ServerSocketChannel关闭,结束进程
		while(isConnected()) {
			TEnv.sleep(1);
		}
	}

	/**
	 *  启动同步监听
	 *  	非阻塞方法
	 * @throws IOException IO异常
	 */
	@Override
	public void syncStart() throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(host, port);
		serverSocketChannel.bind(socketAddress, 1000);
		catchAccept();
	}

	@Override
	protected void acceptStart() throws IOException {
		throw new RuntimeException("Unsupport method");
	}

	@Override
	public boolean isOpen() {
		if(serverSocketChannel!=null) {
			return serverSocketChannel.isOpen();
		}else{
			return false;
		}
	}

	@Override
	public boolean isConnected() {
		if(serverSocketChannel!=null) {
			return serverSocketChannel.isOpen();
		}else{
			return false;
		}
	}

	@Override
	public boolean close(){

		if(serverSocketChannel!=null && serverSocketChannel.isOpen()){
			try{
				//触发 DisConnect 事件
//				EventTrigger.fireDisconnectThread(null);

				//关闭 Socket 连接
				if(serverSocketChannel.isOpen()){
					serverSocketChannel.close();
				}
				return true;
			}catch(IOException e){
				Logger.error("SocketChannel close failed",e);
				return false;
			}
		}else{
			return true;
		}
	}
}
