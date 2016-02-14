package org.voovan.network.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.voovan.network.IoSession;
import org.voovan.network.MessageLoader;
import org.voovan.network.MessageSplitter;
import org.voovan.network.SocketContext;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

/**
 * NIO 会话连接对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class NioSession extends IoSession {
	private SocketChannel		socketChannel;
	private NioSocket			socket;
	private ByteBufferChannel	byteBufferChannel;
	private MessageLoader		messageLoader;

	/**
	 * 构造函数
	 * 
	 * @param socketContext
	 *            socket 上下文对象
	 */
	NioSession(NioSocket nioSocket) {
		super();
		if (nioSocket != null) {
			this.socket = nioSocket;
			this.socketChannel = nioSocket.socketChannel();
			byteBufferChannel = new ByteBufferChannel();
			messageLoader = new MessageLoader(this);
		}else{
			Logger.error("Socket is null, please check it.");
		}

		
	}

	/**
	 * 获取接收的输出流
	 * 
	 * @return
	 */
	protected ByteBufferChannel getByteBufferChannel() {
		return byteBufferChannel;
	}

	/**
	 * 获取本地 IP 地址
	 * 
	 * @return 本地 IP 地址
	 */
	public String loaclAddress() {
		if (socketChannel.isOpen()) {
			return socketChannel.socket().getLocalAddress().getHostAddress();
		} else {
			return null;
		}
	}

	/**
	 * 获取本地端口
	 * 
	 * @return 返回-1为没有取到本地端口
	 */
	public int loaclPort() {
		if (socketChannel.isOpen()) {
			return socketChannel.socket().getLocalPort();
		} else {
			return -1;
		}
	}

	/**
	 * 获取对端 IP 地址
	 * 
	 * @return 对端 ip 地址
	 */
	public String remoteAddress() {
		if (socketChannel.isOpen()) {
			return socketChannel.socket().getInetAddress().getHostAddress();
		} else {
			return null;
		}
	}

	/**
	 * 获取对端端口
	 * 
	 * @return 返回-1为没有取到对端端口
	 */
	public int remotePort() {
		if (socketChannel.isOpen()) {
			return socketChannel.socket().getPort();
		} else {
			return -1;
		}
	}

	/**
	 * 获取SocketChannel 对象
	 * 
	 * @return SocketChannel 对象,连接断开时返回的是null
	 */
	protected SocketChannel socketChannel() {
		if (socketChannel.isOpen()) {
			return socketChannel;
		} else {
			return null;
		}
	}

	/**
	 * 获取 socket 连接上下文
	 * 
	 * @return socket 连接上下文, 连接断开时返回的是null
	 */
	public SocketContext sockContext() {
		return this.socket;
	}

	@Override
	public int read(ByteBuffer buffer) throws IOException {
		int readSize = 0;
		if (isConnect() && buffer != null) {
			try {
				readSize = byteBufferChannel.read(buffer);
			} catch (IOException e) {
				// 如果出现异常则返回-1,表示读取通道结束
				readSize = -1;
			}
		}
		return readSize;
	}

	@Override
	public void send(ByteBuffer buffer) throws IOException {
		if (isConnect() && buffer != null) {
			//循环发送直到全不内容发送完毕
			while(buffer.remaining()!=0){
				socketChannel.write(buffer);
			}
		}
	}

	@Override
	protected MessageLoader getMessageLoader() {
		return messageLoader;
	}

	@Override
	protected MessageSplitter getMessagePartition() {
		return socket.messageSplitter();
	}

	/**
	 * 会话是否打开
	 * 
	 * @return true: 打开,false: 关闭
	 */
	@Override
	public boolean isConnect() {
		return socket.isConnect();
	}

	/**
	 * 关闭会话
	 */
	public boolean close() {
		return socket.Close();
	}

	@Override
	public String toString() {
		return "[" + this.loaclAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
