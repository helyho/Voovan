package org.voovan.network.udp;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.log.Logger;

import java.net.InetSocketAddress;
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
	private InetSocketAddress inetSocketAddress;

	/**
	 * 构造函数
	 *
	 *            socket 上下文对象
	 */
	UdpSession(UdpSocket udpSocket, InetSocketAddress inetSocketAddress) {
		super(udpSocket);
		if (udpSocket != null) {
			this.datagramChannel = udpSocket.socketChannel();
			this.inetSocketAddress = inetSocketAddress;

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
		return datagramChannel.socket().getLocalAddress().getHostAddress();
	}

	/**
	 * 获取本地端口
	 *
	 * @return 返回本地端口
	 */
	public int localPort() {
		return datagramChannel.socket().getLocalPort();
	}

	/**
	 * 获取对端 IP 地址
	 *
	 * @return 对端 ip 地址
	 */
	public String remoteAddress() {
		return inetSocketAddress.getAddress().getHostAddress();
	}

	public InetSocketAddress getInetSocketAddress() {
		return inetSocketAddress;
	}

	public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}


	/**
	 * 获取对端端口
	 *
	 * @return 返回对端端口
	 */
	public int remotePort() {
		return inetSocketAddress.getPort();
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
	protected MessageSplitter getMessageSplitter() {
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
	@Override
	public boolean close() {
		return this.socketContext().close();
	}

	@Override
	public String toString() {
		return "L:[" + this.localAddress() + ":" + this.localPort() + "] <" + (isOpen()?"--":"-X-") + "> R:[" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
