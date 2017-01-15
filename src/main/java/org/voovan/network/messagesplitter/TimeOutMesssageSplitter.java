package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;

public class TimeOutMesssageSplitter implements MessageSplitter {

	private long initTime;
	
	public TimeOutMesssageSplitter(){
		initTime = -1;
	}
	
	@Override
	public int canSplite(IoSession session, byte[] buffer) {
		int timeOut = session.sockContext().getReadTimeout();
		long currentTime = System.currentTimeMillis();
		if(initTime==-1){
			initTime = currentTime;
		} 
		
		if(currentTime-initTime >= timeOut){
			return buffer.length;
		}else{
			return -1;
		}
	}

}
