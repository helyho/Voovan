package org.voovan.network.filter;

import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;

/**
 * Byte数据过滤器
 *      encode 传入为 byte[]
 *      decode 返回为 byte[]
 *      0+4位为数据长度+0+数据
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ByteFilter implements IoFilter {
	public static Class BYTE_ARRAY_CLASS = (new byte[0]).getClass();
	public static int HEAD_LEGNTH = 6;

	@Override
	public Object encode(IoSession session, Object object) {
		if(object.getClass() == BYTE_ARRAY_CLASS){

			byte[] data = (byte[])object;
			ByteBuffer byteBuffer = ByteBuffer.allocate(HEAD_LEGNTH + data.length);
			byteBuffer.put((byte)0);
			byteBuffer.putInt(data.length);
			byteBuffer.put((byte)0);
			byteBuffer.put(data);
			byteBuffer.flip();
			return byteBuffer;
		}
		return null;
	}

	@Override
	public Object decode(IoSession session,Object object) {
		if(object instanceof ByteBuffer){
			boolean success = false;

			ByteBuffer byteBuffer = (ByteBuffer) object;
			int originPosition = byteBuffer.position();
			try {
				if(byteBuffer.remaining() < 4){
					Logger.error("ByteFilter decode error: Not enough data length, socket will be close");
					session.close();
				}

				if (byteBuffer.get() == 0) {
					int length = byteBuffer.getInt();
					if (byteBuffer.get() == 0) {
						if (length > 0) {
							byte[] data = new byte[length];
							byteBuffer.get(data);
							success = true;
							return data;
						}
					} else {
						Logger.error("ByteFilter decode error: payloadLength end not exists, socket will be close");
						session.close();
					}
				} else {
					Logger.error("ByteFilter decode error: payloadLength head not exists, socket will be close");
					session.close();
				}
			} finally {
				if(!success){
					byteBuffer.position(originPosition);
				}
			}
		}
		return null;
	}
}
