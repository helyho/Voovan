package org.voovan.network.messagesplitter;

import org.voovan.Global;
import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.buffer.TByteBuffer;

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
	public byte[] bytes = Global.STR_LF.getBytes();

	@Override
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {
		if(byteBuffer.limit() > 1){
			int lineBreakIndex = TByteBuffer.indexOf(byteBuffer, bytes);
			if(lineBreakIndex >=0 )
				return lineBreakIndex+1;
		}
		return -1;
	}

}
