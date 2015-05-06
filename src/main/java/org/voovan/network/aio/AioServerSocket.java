package org.voovan.network.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;

import org.voovan.network.EventTrigger;
import org.voovan.network.SocketContext;
import org.voovan.tools.TEnv;

/**
 * AioServerSocket 监听
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class AioServerSocket extends SocketContext{

	private AsynchronousServerSocketChannel serverSocketChannel;
	private EventTrigger eventTrigger;
	
	/**
	 * 构造函数
	 * @param host
	 * @param port
	 * @param readTimeout
	 * @throws IOException
	 */
	public AioServerSocket(String host,int port,int readTimeout) throws IOException{
		super(host, port, readTimeout);
		serverSocketChannel = AsynchronousServerSocketChannel.open();
		eventTrigger = new EventTrigger(null);
		
	}
	
	/**
	 * 捕获 Aio Accept 事件
	 */
	public void catchAccept(){
		serverSocketChannel.accept(this, new AcceptCompletionHandler(eventTrigger));
	}
	
	@Override
	public void start() throws IOException {
		InetSocketAddress socketAddress = new InetSocketAddress(host, port);
		serverSocketChannel.bind(socketAddress, 1000);
		catchAccept();
		
		//等待ServerSocketChannel关闭,结束进程
		while(isConnect() && !eventTrigger.isShutdown()){
			TEnv.sleep(500);
		}
	}

	@Override
	public boolean isConnect() {
		return serverSocketChannel.isOpen();
	}
	
	@Override
	public boolean Close(){
		
		if(serverSocketChannel!=null && serverSocketChannel.isOpen()){
			try{
				//触发 DisConnect 事件
				eventTrigger.fireDisconnect();
				//检查是否关闭
				eventTrigger.shutdown();
				//关闭 Socket 连接
				if(serverSocketChannel.isOpen()  && eventTrigger.isShutdown()){
					serverSocketChannel.close();
				}
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
