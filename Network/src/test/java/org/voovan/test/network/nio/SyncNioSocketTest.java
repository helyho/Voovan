package org.voovan.test.network.nio;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.nio.NioSocket;
import org.voovan.tools.log.Logger;

public class SyncNioSocketTest {
	
	public static void main(String[] args) throws Exception {
		NioSocket socket = new NioSocket("127.0.0.1",2031,5000);
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
	    socket.syncStart();
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
