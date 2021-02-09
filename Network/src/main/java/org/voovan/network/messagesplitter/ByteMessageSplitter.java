package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.network.filter.ByteFilter;

import java.nio.ByteBuffer;

/**
 * Byte数据截断器
 *      255+4位为数据长度+255+数据
 *
 * @author: helyho
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
					//TODO: 自动校正到正确的消息便宜位置
					int length = byteBuffer.getInt();

					if (byteBuffer.get() == ByteFilter.SPLITER) {
						if (length > 0 && byteBuffer.remaining() >= length) {
							ret = ByteFilter.HEAD_LEGNTH + length;
						}
					}
				}
			}
		} finally {
			byteBuffer.position(originPosition);
		}

		if(ret < 0) {
			session.close();
		}

		return ret;
	}
}
