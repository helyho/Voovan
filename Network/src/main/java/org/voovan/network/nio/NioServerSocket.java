package org.voovan.network.nio;

import org.voovan.network.SocketContext;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
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
public class NioServerSocket extends SocketContext{
	
	private SelectorProvider provider;
	private Selector selector;
	private ServerSocketChannel serverSocketChannel;
	 
	 
	/**
	 * 构造函数
	 * @param host      监听地址
	 * @param port		监听端口
	 * @param readTimeout   超时时间
	 * @throws IOException	异常
	 */
	public NioServerSocket(String host,int port,int readTimeout) throws IOException{
		super(host, port, readTimeout);
		provider = SelectorProvider.provider();
		serverSocketChannel = provider.openServerSocketChannel();
		serverSocketChannel.socket().setSoTimeout(this.readTimeout);
		serverSocketChannel.configureBlocking(false);
		init();
	}
	
	
	/**
	 * 获取 SocketChannel 对象
	 * @return SocketChannel 对象
	 */
	public ServerSocketChannel socketChannel(){
		return this.serverSocketChannel;
	}
	
	/**
	 * 初始化函数
	 * @throws IOException
	 */
	private void init() throws IOException{
		selector = provider.openSelector();
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		serverSocketChannel.bind(new InetSocketAddress(host, port));
	}
	
	/**
	 * 启动
	 * @throws IOException  IO 异常
	 */
	@Override
	public void start() throws IOException {
		NioSelector eventListener = new NioSelector(selector,this);
		eventListener.eventChose();
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
