package org.voovan.network.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

import org.voovan.network.IoSession;
import org.voovan.network.MessageLoader;
import org.voovan.network.MessageSplitter;
import org.voovan.network.SocketContext;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
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
public class AioSession extends IoSession {

	private AsynchronousSocketChannel	socketChannel;
	private AioSocket					socket;
	private ByteBufferChannel			byteBufferChannel;
	private MessageLoader				messageLoader;

	/**
	 * 构造函数
	 * 
	 * @param socket
	 * @param readTimeout
	 */
	AioSession(AioSocket socket) {
		super();
		if (socket != null) {
			this.socketChannel = socket.socketChannel();
			this.socket = socket;
			byteBufferChannel = new ByteBufferChannel();
		    this.messageLoader = new MessageLoader(this);
		} else {
			Logger.error("SocketChannel is null, please check it.");
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

	@Override
	public String loaclAddress() {
		if (this.isConnect()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getLocalAddress());
				return socketAddress.getHostName();
			} catch (IOException e) {
				Logger.error("Get SocketChannel local address failed.",e);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public int loaclPort() {
		if (this.isConnect()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getLocalAddress());
				return socketAddress.getPort();
			} catch (IOException e) {
				Logger.error("Get SocketChannel local port failed.",e);
				return -1;
			}
		} else {
			return -1;
		}
	}

	@Override
	public String remoteAddress() {
		if (this.isConnect()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getRemoteAddress());
				return socketAddress.getHostString();
			} catch (IOException e) {
				Logger.error("Get SocketChannel remote address failed.",e);
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public int remotePort() {
		if (this.isConnect()) {
			try {
				InetSocketAddress socketAddress = TObject.cast(socketChannel.getRemoteAddress());
				return socketAddress.getPort();
			} catch (IOException e) {
				Logger.error("Get SocketChannel remote port failed.",e);
				return -1;
			}
		} else {
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
		if (isConnect() && buffer != null) {
			try {
				readSize = byteBufferChannel.read(buffer);
			} catch (IOException e) {
				Logger.error("Read socketChannel failed.",e);
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
				Future<Integer> sendResult = socketChannel.write(buffer);
				while(!sendResult.isDone()){
					TEnv.sleep(1);
				}
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

	@Override
	public boolean isConnect() {

		return socket.isConnect();
	}

	@Override
	public boolean close() {
		// 关闭 socket
		return socket.Close();
	}

	@Override
	public String toString() {
		return "[" + this.loaclAddress() + ":" + this.loaclPort() + "] -> [" + this.remoteAddress() + ":" + this.remotePort() + "]";
	}
}
