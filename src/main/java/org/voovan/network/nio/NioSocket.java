package org.voovan.network.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

import javax.net.ssl.SSLException;

import org.voovan.network.ConnectModel;
import org.voovan.network.SocketContext;
import org.voovan.tools.log.Logger;

/**
 * NioSocket 连接
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class NioSocket extends SocketContext{
	private SelectorProvider provider;
	private Selector selector;
	private SocketChannel socketChannel;
	private NioSession session;
	
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
		session = new NioSession(this);
		connectModel = ConnectModel.CLIENT;
		init();
	}
	
	/**
	 * 构造函数
	 * @param socketChannel SocketChannel 对象
	 */
	protected NioSocket(SocketContext parentSocketContext,SocketChannel socketChannel){
		try {
			provider = SelectorProvider.provider();
			this.host = socketChannel.socket().getLocalAddress().getHostAddress();
			this.port = socketChannel.socket().getLocalPort();
			this.socketChannel = socketChannel;
			socketChannel.configureBlocking(false);
			this.copyFrom(parentSocketContext);
			this.socketChannel().socket().setSoTimeout(this.readTimeout);
			session = new NioSession(this);
			connectModel = ConnectModel.SERVER;
			init();
		} catch (IOException e) {
			Logger.error("Create socket channel failed",e);
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
		catch(IOException e){
			Logger.error("init SocketChannel failed by openSelector",e);
		}
	}
	
	public NioSession getSession(){
		return session;
	}
	
	private void initSSL() throws SSLException{
		if (connectModel == ConnectModel.SERVER && sslManager != null) {
			sslManager.createServerSSLParser(session);
		} else if (connectModel == ConnectModel.CLIENT && sslManager != null) {
			sslManager.createClientSSLParser(session);
		}
	}
	
	/**
	 * 启动
	 * @throws IOException 
	 */
	public void start() throws IOException  {
		initSSL();
		
		if(socketChannel!=null && socketChannel.isOpen()){
			NioSelector nioSelector = new NioSelector(selector,this);
			nioSelector.eventChose();
		}
	}

	@Override
	public boolean isConnect() {
		if(socketChannel!=null){
			return socketChannel.isOpen();
		}
		else{
			return false;
		}
	}

	@Override
	public boolean Close(){
		if(socketChannel!=null){
			try{
				socketChannel.close();
				return true;
			} catch(IOException e){
				Logger.error("Close SocketChannel failed",e);
				return false;
			}
		}else{
			return true;
		}
	}

}
