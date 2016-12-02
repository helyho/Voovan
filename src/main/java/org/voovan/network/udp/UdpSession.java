package org.voovan.network.udp;

import org.voovan.network.IoSession;
import org.voovan.network.MessageLoader;
import org.voovan.network.MessageSplitter;
import org.voovan.network.SocketContext;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * UDP NIO 会话连接对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UdpSession extends IoSession {
	private DatagramChannel	datagramChannel;
	private UdpSocket			udpSocket;
	private ByteBufferChannel	byteBufferChannel;
	private MessageLoader		messageLoader;
	private InetSocketAddress remoteAddress;

	/**
	 * 构造函数
	 *
	 *            socket 上下文对象
	 */
	UdpSession(UdpSocket udpSocket, InetSocketAddress remoteAddress) {
		super();
		if (udpSocket != null) {
			this.udpSocket = udpSocket;
			this.datagramChannel = udpSocket.datagramChannel();
			byteBufferChannel = new ByteBufferChannel();
			messageLoader = new MessageLoader(this);
			this.remoteAddress = remoteAddress;
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
		if (datagramChannel.isOpen()) {
			return datagramChannel.socket().getLocalAddress().getHostAddress();
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
		if (datagramChannel.isOpen()) {
			return datagramChannel.socket().getLocalPort();
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
		if (datagramChannel.isOpen()) {
			return remoteAddress.getAddress().getHostAddress();
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
		if (datagramChannel.isOpen()) {
			return remoteAddress.getPort();
		} else {
			return -1;
		}
	}

	/**
	 * DatagramChannel 对象
	 * 
	 * @return DatagramChannel 对象,连接断开时返回的是null
	 */
	protected DatagramChannel datagramChannel() {
		if (datagramChannel.isOpen()) {
			return datagramChannel;
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
		return this.udpSocket;
	}

	@Override
	public int read(ByteBuffer buffer) throws IOException {
		int readSize = 0;
		if (buffer != null) {
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
	public int send(ByteBuffer buffer) throws IOException {
		int totalSendByte = 0;
		if (isConnect() && buffer != null) {
			//循环发送直到全不内容发送完毕
			while(isConnect() && buffer.remaining()!=0){
				totalSendByte+=datagramChannel.send(buffer, remoteAddress);
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
		return udpSocket.messageSplitter();
	}

	/**
	 * 会话是否打开
	 * 
	 * @return true: 打开,false: 关闭
	 */
	@Override
	public boolean isConnect() {
		return udpSocket.isConnect();
	}

	/**
	 * 关闭会话
	 */
	public boolean close() {
		return udpSocket.close();
	}

	@Override
	public String toString() {
		return "[" + this.loaclAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
