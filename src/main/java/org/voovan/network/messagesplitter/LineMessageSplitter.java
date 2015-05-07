package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;

/**
 * 按换行对消息分割
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class LineMessageSplitter implements MessageSplitter {

	@Override
	public boolean canSplite(IoSession session, byte[] buffer, int elapsedtime) {
		if(buffer[buffer.length]=='\n'){
			return true;
		}
		return false;
	}
	
}
