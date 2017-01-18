package org.voovan.test.network.performTest;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TByteBuffer;

import java.nio.ByteBuffer;

public class PerformTestSpliter implements MessageSplitter {

	@Override
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {
		String requestStr = TByteBuffer.toString(byteBuffer);
		if(requestStr.endsWith("\r\n\r\n")){
			return byteBuffer.limit();
		}else{
			return -1;
		}
	}
	 
}
