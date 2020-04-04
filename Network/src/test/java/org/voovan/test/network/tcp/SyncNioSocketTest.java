package org.voovan.test.network.tcp;

import org.voovan.network.filter.StringFilter;
import org.voovan.network.messagesplitter.LineMessageSplitter;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

public class SyncNioSocketTest {
	
	public static void main(String[] args) throws Exception {
		TcpSocket socket = new TcpSocket("127.0.0.1",2031,5000);
		socket.filterChain().add(new StringFilter());
		socket.messageSplitter(new LineMessageSplitter());
	    socket.syncStart();

		socket.syncSend("syncSocket\r\n");
		try {
			System.out.println("Server: " + socket.syncRead());
		}catch (Exception e){
			e.printStackTrace();
		}

		socket.syncSend("syncSocket read after recieve\r\n");
		try {
			TEnv.sleep(1000);
			System.out.println("Server: " + socket.syncRead());
		}catch (Exception e){
			e.printStackTrace();
		}
		socket.close();
		Logger.simple("==================================Terminate==================================");
	}
}
