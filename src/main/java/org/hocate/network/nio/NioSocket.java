package org.hocate.network.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import org.hocate.network.ConnectModel;
import org.hocate.network.SocketContext;

/**
 * NioSocket 连接
 * @author helyho
 *
 */
public class NioSocket extends SocketContext{
	private SelectorProvider provider;
	private Selector selector;
	private SocketChannel socketChannel;
	private NioSession session;
	private ConnectModel connectModel;
	
	/**
	 * socket 连接
	 * @param addr      监听地址
	 * @param port		监听端口
	 * @param readTimeout   超时事件
	 * @throws IOException	异常
	 */
	public NioSocket(String host,int port,int readTimeout) throws IOException{
		super(host, port, readTimeout);
		this.readTimeout = readTimeout;
		provider = SelectorProvider.provider();
		socketChannel = provider.openSocketChannel();
		socketChannel.socket().setSoTimeout(this.readTimeout);
		socketChannel.connect(new InetSocketAddress(this.host,this.port));
		socketChannel.configureBlocking(false);
		session = new NioSession(this,readTimeout);
		connectModel = ConnectModel.CLIENT;
		init();
	}
	
	/**
	 * 构造函数
	 * @param socketChannel SocketChannel 对象
	 */
	public NioSocket(SocketContext parentSocketContext,SocketChannel socketChannel){
		try {
			provider = SelectorProvider.provider();
			this.host = socketChannel.socket().getLocalAddress().getHostAddress();
			this.port = socketChannel.socket().getLocalPort();
			this.socketChannel = socketChannel;
			socketChannel.configureBlocking(false);
			this.cloneInit(parentSocketContext);
			this.socketChannel().socket().setSoTimeout(this.readTimeout);
			session = new NioSession(this,this.readTimeout);
			connectModel = ConnectModel.SERVER;
			init();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 获取 SocketChannel 对象
	 * @return
	 */
	public SocketChannel socketChannel(){
		return this.socketChannel;
	}
	
	/**
	 * 初始化函数
	 */
	private void init()  {
		try{
			selector = provider.openSelector();
			socketChannel.register(selector, SelectionKey.OP_READ);
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public NioSession getSession(){
		return session;
	}
	
	/**
	 * 启动
	 * @throws IOException 
	 */
	public void start() throws Exception {
		if(connectModel == ConnectModel.SERVER && sslManager != null){
			sslManager.createServerSSLParser(session);
		}
		else if(connectModel == ConnectModel.CLIENT && sslManager != null){
			sslManager.createClientSSLParser(session);
		}	
		
		if(socketChannel!=null && socketChannel.isOpen()){
			NioSelector eventListener = new NioSelector(selector,this);
			eventListener.EventChose();
		}
	}

	@Override
	public synchronized boolean isConnect() {
		if(socketChannel!=null){
			return socketChannel.isOpen();
		}
		else{
			return false;
		}
	}

	@Override
	public boolean Close(){
		if(socketChannel!=null && socketChannel.isOpen()){
			try{
				socketChannel.close();
				return true;
			}
			catch(Exception e){
				e.printStackTrace();
				return false;
			}
		}else{
			return true;
		}
	}

}
