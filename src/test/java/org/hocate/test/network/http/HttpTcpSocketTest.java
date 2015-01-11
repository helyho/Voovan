package org.hocate.test.network.http;

import org.hocate.network.aio.AioSocket;

public class HttpTcpSocketTest {
	
	public static void main(String[] args) throws Exception {
		String hostString = "www.baidu.com";
		AioSocket socket = new AioSocket(hostString,80,500);
		socket.handler(new HttpRequestHandlerTest(hostString));
		//socket.filterChain().add(new StringFilter());
		socket.start();
	}
}
