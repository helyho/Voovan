package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.network.filter.ByteFilter;
import org.voovan.tools.TByte;
import org.voovan.tools.Varint;
import org.voovan.tools.buffer.TByteBuffer;

import java.nio.ByteBuffer;

/**
 * Byte数据截断器
 *      255+4位为数据长度+255+数据
 *
 * @author helyho
 * Project: DBase
 * Create: 2017/11/1 14:38
 */
public class ByteMessageSplitter implements MessageSplitter {
	@Override
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {
		int originPosition = byteBuffer.position();

		int ret = -1;

		try {
			if (byteBuffer.remaining() > ByteFilter.HEAD_LEGNTH) {
				if(byteBuffer.get() == ByteFilter.SPLITER) {
					int lengthByteOffset = byteBuffer.position();
					int length = Varint.varintToInt(byteBuffer);
					int lengthByteSize = byteBuffer.position() - lengthByteOffset;

					if(length < 0){
						session.close();
					}

					if(byteBuffer.hasRemaining()) {
						if (byteBuffer.get() == ByteFilter.SPLITER) {
							if (length > 0 && byteBuffer.remaining() >= length) {
								ret = ByteFilter.HEAD_LEGNTH + lengthByteSize + length;
							}
						} else {
							session.close();
						}
					}
				} else {
					session.close();
				}
			}
		} finally {
			byteBuffer.position(originPosition);
		}

		return ret;
	}
}
