package org.voovan.network.tcp;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.network.exception.RestartException;
import org.voovan.tools.log.Logger;

import java.io.IOException;
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
public class TcpSession extends IoSession<TcpSocket> {
	private SocketChannel		socketChannel;

	/**
	 * 构造函数
	 *
	 *            socket 上下文对象
	 */
	TcpSession(TcpSocket tcpSocket) {
		super(tcpSocket);
		if (tcpSocket != null) {
			socketChannel = tcpSocket.socketChannel();
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
	@Override
	public boolean close() {
		return this.socketContext().close();
	}

	@Override
	public String toString() {
		return "[" + this.localAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
