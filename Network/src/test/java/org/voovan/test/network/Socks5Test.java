package org.voovan.test.network;

import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.plugin.SSLPlugin;
import org.voovan.network.plugin.Socks5Plugin;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.test.network.ClientHandlerTest;
import org.voovan.tools.TEnv;
import org.voovan.tools.buffer.TByteBuffer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Socks5Test {

	public static void main(String[] args) throws Exception {
		TcpSocket socket = new TcpSocket("127.0.0.1", 1080, 5000 * 60, 1);
		socket.pluginChain()
				.add(new Socks5Plugin(new InetSocketAddress("www.google.com", 443)))
				.add(new SSLPlugin());
		socket.syncStart();


		IoSession session = socket.getSession();

		String reqData = "GET / HTTP/1.1\r\n" +
				"Host: www.google.com\r\n" +
				"Connection: keep-alive\r\n" +
				"Cache-Control: max-age=0\r\n" +
				"Upgrade-Insecure-Requests: 1\r\n" +
				"User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36\r\n" +
				"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9\r\n" +
				"Accept-Language: zh-CN,zh;q=0.9,en;q=0.8\r\n" +
				"Cookie: _ga=GA1.1.383879526.1585421546; _ga_NSJH3FW0LK=GS1.1.1620788011.10.1.1620788300.0\r\n\r\n";
		ByteBuffer byteBuffer = ByteBuffer.allocate(reqData.length());
		byteBuffer.put(reqData.getBytes());
		byteBuffer.flip();
		session.send(byteBuffer, true);

		ByteBuffer byteBufferResp = (ByteBuffer) session.syncRead();

		System.out.println(TByteBuffer.toString(byteBufferResp));
	}


	public static class InnerHandler implements IoHandler {

		@Override
		public Object onConnect(IoSession session) {
			System.out.println("onConnect");
			return null;
		}

		@Override
		public void onDisconnect(IoSession session) {
			System.out.println("onDisconnect");

		}

		@Override
		public Object onReceive(IoSession session, Object obj) {
			System.out.println("onReceive");
			return null;
		}

		@Override
		public void onSent(IoSession session, Object obj) {
			System.out.println("onSent");

		}

		@Override
		public void onFlush(IoSession session) {
			System.out.println("onFlush");

		}

		@Override
		public void onException(IoSession session, Exception e) {
			System.out.println("onException");

		}

		@Override
		public void onIdle(IoSession session) {
			System.out.println("onIdle");

		}
	}
}
