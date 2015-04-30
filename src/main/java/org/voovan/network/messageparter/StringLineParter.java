package org.voovan.network.messageparter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageParter;

/**
 * 按换行对消息分割
 * 
 * @author helyho
 *
 */
public class StringLineParter implements MessageParter {

	@Override
	public boolean canPartition(IoSession session, byte[] buffer, int elapsedtime) {

		for(int i=0;i<buffer.length;i++){
			if(buffer[i]=='\r' || buffer[i]=='\n'){
				return true;
			}
		}
		return false;
	}
	
}
