package org.voovan.test.network.performTest;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;

public class PerformTestSpliter implements MessageSplitter {

	@Override
	public boolean canSplite(IoSession session, byte[] buffer) {
		String requestStr = new String(buffer);
		if(requestStr.endsWith("\r\n\r\n")){
			return true;
		}else{
			return false;
		}
	}
	 
}
