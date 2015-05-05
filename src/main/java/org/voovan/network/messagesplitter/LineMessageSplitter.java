package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;

/**
 * 按换行对消息分割
 * 
 * @author helyho
 *
 */
public class LineMessageSplitter implements MessageSplitter {

	@Override
	public boolean canSplite(IoSession session, byte[] buffer, int elapsedtime) {

		for(int i=0;i<buffer.length;i++){
			if(buffer[i]=='\r' || buffer[i]=='\n'){
				return true;
			}
		}
		return false;
	}
	
}
