package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.network.filter.ByteFilter;

import java.nio.ByteBuffer;

/**
 * Byte数据截断器
 *      0+4位为数据长度+0+数据
 *
 * @author: helyho
 * Project: DBase
 * Create: 2017/11/1 14:38
 */
public class ByteMessageSpliter implements MessageSplitter {
	@Override
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {
		int originPosition = byteBuffer.position();

		try {
			if (byteBuffer.get() == 0) {
				int length = byteBuffer.getInt();

				if (byteBuffer.get() == 0) {
					if (length > 0 && byteBuffer.remaining() >= length) {
						return ByteFilter.HEAD_LEGNTH + length;
					}
				}
			}
		} finally {
			byteBuffer.position(originPosition);
		}

		return -1;
	}
}
