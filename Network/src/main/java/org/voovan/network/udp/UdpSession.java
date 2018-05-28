package org.voovan.network.udp;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.network.exception.RestartException;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
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
public class UdpSession extends IoSession<UdpSocket> {
	private DatagramChannel	datagramChannel;
	private InetSocketAddress remoteAddress;

	/**
	 * 构造函数
	 *
	 *            socket 上下文对象
	 */
	UdpSession(UdpSocket udpSocket, InetSocketAddress remoteAddress) {
		super(udpSocket);
		if (udpSocket != null) {
			this.datagramChannel = udpSocket.datagramChannel();
			this.remoteAddress = remoteAddress;

			//UDP 是无状态协议,所以无心跳,也无空闲事件,所以这里设置为0
			this.setIdleInterval(0);
		}else{
			Logger.error("Socket is null, please check it.");
		}


	}

	/**
	 * 获取本地 IP 地址
	 *
	 * @return 本地 IP 地址
	 */
	public String localAddress() {
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

	@Override
	protected int read0(ByteBuffer buffer) throws IOException {
		int readSize = 0;
		if (buffer != null) {
			readSize = this.getByteBufferChannel().readHead(buffer);
		}
		return readSize;
	}

	@Override
	protected synchronized int send0(ByteBuffer buffer) throws IOException {
		int totalSendByte = 0;
		int waitCount = 0;
		if (isOpen() && buffer != null) {
			//循环发送直到全部内容发送完毕
			while(buffer.remaining()!=0){
				int sendSize = 0;
				if(datagramChannel.getRemoteAddress()!=null) {
					datagramChannel.write(buffer);
				} else {
					datagramChannel.send(buffer, this.remoteAddress);
				}
				if(sendSize == 0 ){
					waitCount++;
					TEnv.sleep(1);
					if(waitCount >= socketContext().getSendTimeout()){
						Logger.error("AioSession send timeout, Socket will be close");
						close();
						return -1;
					}
				} else {
					waitCount = 0;
					totalSendByte += sendSize;
				}
			}
		}
		return totalSendByte;
	}

	@Override
	protected MessageSplitter getMessagePartition() {
		return this.socketContext().messageSplitter();
	}

	/**
	 * 会话是否打开
	 *
	 * @return true: 打开,false: 关闭
	 */
	@Override
	public boolean isConnected() {
		return this.socketContext().isConnected();
	}

	/**
	 * 会话是否打开
	 *
	 * @return true: 打开,false: 关闭
	 */
	@Override
	public boolean isOpen() {
		return this.socketContext().isOpen();
	}

	/**
	 * 关闭会话
	 */
	public boolean close() {
		this.cancelIdle();
		return this.socketContext().close();
	}

	/**
	 * 重连当前连接
	 * @throws IOException IO 异常
	 * @throws RestartException 重新启动的异常
	 */
	public void restart() throws IOException, RestartException {
		socketContext().restart();
	}

	@Override
	public String toString() {
		return "[" + this.localAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
