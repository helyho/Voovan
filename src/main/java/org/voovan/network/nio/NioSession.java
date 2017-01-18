package org.voovan.network.nio;

import org.voovan.network.IoSession;
import org.voovan.network.MessageLoader;
import org.voovan.network.MessageSplitter;
import org.voovan.network.SocketContext;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

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
	 *            socket 上下文对象
	 */
	NioSession(NioSocket nioSocket) {
		super();
		if (nioSocket != null) {
			this.socket = nioSocket;
			this.socketChannel = nioSocket.socketChannel();
			byteBufferChannel = new ByteBufferChannel(this.sockContext().getBufferSize());
			messageLoader = new MessageLoader(this);
		}else{
			Logger.error("Socket is null, please check it.");
		}

		
	}

	/**
	 * 获取接收的输出流
	 * 
	 * @return 接收的输出流
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
		if (buffer != null) {
			try {
				readSize = byteBufferChannel.readHead(buffer);
			} catch (Exception e) {
				// 如果出现异常则返回-1,表示读取通道结束
				readSize = -1;
			}
		}
		return readSize;
	}

	@Override
	public int send(ByteBuffer buffer) throws IOException {
		int totalSendByte = 0;
		if (isConnected() && buffer != null) {
			//循环发送直到全不内容发送完毕
			while(isConnected() && buffer.remaining()!=0){
				totalSendByte+=socketChannel.write(buffer);
			}
		}
		return totalSendByte;
	}

	/**
	 * 打开直接读取模式
	 */
	public void openDirectBufferRead(){
		messageLoader.setDirectRead(true);
	}

	/**
	 * 直接从缓冲区读取数据
	 * @return 字节缓冲对象ByteBuffer
	 * @throws IOException IO异常
	 * */
	public ByteBuffer directBufferRead() throws IOException {
		messageLoader.setDirectRead(true);
		return  messageLoader.directRead();
	}

	/**
	 * 关闭直接读取模式
	 */
	public void closeDirectBufferRead(){
		messageLoader.setDirectRead(false);
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
	public boolean isConnected() {
		return socket.isConnected();
	}

	/**
	 * 会话是否打开
	 *
	 * @return true: 打开,false: 关闭
	 */
	@Override
	public boolean isOpen() {
		return socket.isOpen();
	}

	/**
	 * 关闭会话
	 */
	public boolean close() {
		return socket.close();
	}

	@Override
	public String toString() {
		return "[" + this.loaclAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
