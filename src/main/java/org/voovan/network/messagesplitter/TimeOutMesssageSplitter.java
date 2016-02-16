package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;

public class TimeOutMesssageSplitter implements MessageSplitter {

	private long initTime;
	
	public TimeOutMesssageSplitter(){
		initTime = -1;
	}
	
	@Override
	public boolean canSplite(IoSession session, byte[] buffer) {
		int timeOut = session.sockContext().getReadTimeout();
		long currentTime = System.currentTimeMillis();
		if(initTime==-1){
			initTime = currentTime;
		} 
		
		return currentTime-initTime >= timeOut;
	}

}
