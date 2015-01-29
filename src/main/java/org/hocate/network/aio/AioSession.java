package org.hocate.network.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

import org.hocate.network.ByteBufferChannel;
import org.hocate.network.IoSession;
import org.hocate.network.MessageLoader;
import org.hocate.network.MessageParter;
import org.hocate.network.SocketContext;
import org.hocate.tools.TObject;

public class AioSession extends IoSession{

	private AsynchronousSocketChannel socketChannel;
	private AioSocket socket;
	private ByteBufferChannel byteBufferChannel;
	private MessageLoader messageLoader; 
	
	/**
	 * 构造函数
	 * @param socket
	 * @param readTimeout
	 */
	public AioSession(AioSocket socket,int readTimeout){
		super();
		this.socketChannel = socket.socketChannel();
		this.socket = socket;
		
		if(socket!=null){
			byteBufferChannel = new ByteBufferChannel();
		}
		
		this.messageLoader = new MessageLoader(this, readTimeout,1024);
	}
	
	/**
	 * 获取接收的输出流
	 * @return
	 */
	protected ByteBufferChannel getByteBufferChannel(){
		return byteBufferChannel;
	}
	
	
	@Override
	public String loaclAddress() {
		try {
			InetSocketAddress socketAddress = TObject.cast(socketChannel.getLocalAddress());
			return socketAddress.getHostName();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int loaclPort() {
		try {
			InetSocketAddress socketAddress = TObject.cast(socketChannel.getLocalAddress());
			return socketAddress.getPort();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public String remoteAddress() {
		try {
			InetSocketAddress socketAddress = TObject.cast(socketChannel.getRemoteAddress());
			return socketAddress.getHostString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public int remotePort() {
		try {
			InetSocketAddress socketAddress = TObject.cast(socketChannel.getRemoteAddress());
			return socketAddress.getPort();
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@Override
	public SocketContext sockContext() {
		return socket;
	}

	@Override
	protected int read(ByteBuffer buffer) throws IOException {
		int readSize = 0;
		if(isConnect() && buffer!=null){
			try{
				readSize = byteBufferChannel.read(buffer);
			}
			catch(Exception e){
				//如果出现异常则返回-1,表示读取通道结束
				readSize = -1;
			}
		}
		return readSize;
	}
	
	@Override
	public void send(ByteBuffer buffer) throws IOException {
		if(isConnect() && buffer!=null){
			do{
				socketChannel.write(buffer);
			}while(buffer.hasRemaining());
		}
	}
	
	@Override
	protected MessageLoader getMessageLoader() {
		return messageLoader;
	}
	
	@Override
	protected MessageParter getMessagePartition() {
		return socket.messageParter();
	}
	
	@Override
	public boolean isConnect() {
		
		return socket.isConnect();
	}

	@Override
	public boolean close() {
		//关闭 socket
		return socket.Close();
	}

	@Override
	public String toString() {
		return "["+this.loaclAddress()+":"+this.loaclPort()+"] -> ["+this.remoteAddress()+":"+this.remotePort()+"]";
	}
}
