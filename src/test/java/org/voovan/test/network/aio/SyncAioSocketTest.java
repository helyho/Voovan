package org.voovan.test.network.aio;

import org.voovan.network.SynchronousHandler;
import org.voovan.network.aio.AioSocket;
import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.test.network.ClientHandlerTest;
import org.voovan.tools.log.Logger;

public class SyncAioSocketTest {
	
	public static void main(String[] args) throws Exception {
		AioSocket socket = new AioSocket("127.0.0.1",2031,30000);
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
		socket.start();
		socket.synchronouSend("syncSocket\r\n");
		try {
			System.out.println(socket.synchronouRead());
		}catch (Exception e){
			e.printStackTrace();
		}
		socket.close();
		Logger.simple("Terminate");
	}
}
