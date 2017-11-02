package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TByteBuffer;

import java.nio.ByteBuffer;

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
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {
		if(byteBuffer.limit() > 1){
			int lineBreakIndex = TByteBuffer.indexOf(byteBuffer, "\n".getBytes());
			if(lineBreakIndex >=0 )
				return lineBreakIndex+1;
		}
		return -1;
	}

}
