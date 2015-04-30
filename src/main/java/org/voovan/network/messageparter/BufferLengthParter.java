package org.voovan.network.messageparter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageParter;

/**
 * 按定长对消息分割
 * 		超过指定长度也会分割
 * @author helyho
 *
 */
public class BufferLengthParter implements MessageParter {
	private long bufferLength;
	
	public BufferLengthParter(long bufferLength){
		this.bufferLength = bufferLength;
	}

	@Override
	public boolean canPartition(IoSession session, byte[] buffer, int elapsedtime) {
		if(buffer.length>=bufferLength){
			return true;
		}
		else{
			return false;
		}
	}
	
}
