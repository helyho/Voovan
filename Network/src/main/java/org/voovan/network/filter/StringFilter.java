package org.voovan.network.filter;

import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.tools.TByteBuffer;

import java.nio.ByteBuffer;

/**
 * String 过滤器
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class StringFilter implements IoFilter {
	private boolean isByteBuffer;

	/**
	 * 构造函数
	 * @param isByteBuffer 过滤类型是否是 ByteBuffer, true: 过滤类型 Bytebuffer, false: 过滤类型byte
	 */
	public StringFilter(boolean isByteBuffer){
		this.isByteBuffer = isByteBuffer;
	}

	public StringFilter(){
		this.isByteBuffer = true;
	}

	@Override
	public Object encode(IoSession session, Object object) {
		if(object instanceof String){
			String sourceString = object.toString();
			if(isByteBuffer) {
				return ByteBuffer.wrap(sourceString.getBytes());
			} else {
				return sourceString.getBytes();
			}
		}

		return object;
	}

	@Override
	public Object decode(IoSession session,Object object) {
		if(object instanceof ByteBuffer && isByteBuffer){
            return TByteBuffer.toString((ByteBuffer)object);
		}

		if(object.getClass() == ByteFilter.BYTE_ARRAY_CLASS && !isByteBuffer){
			return new String((byte[])object);
		}

		return object;
	}
}
