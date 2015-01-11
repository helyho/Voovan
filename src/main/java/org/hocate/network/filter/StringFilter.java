package org.hocate.network.filter;

import java.nio.ByteBuffer;

import org.hocate.network.IoFilter;
import org.hocate.network.MessageLoader;
import org.hocate.tools.TObject;

/**
 * String 过滤器
 * @author helyho
 *
 */
public class StringFilter implements IoFilter {

	@Override
	public Object encode(Object object) {
		if(object instanceof String){
			String sourceString = TObject.cast(object);
			return ByteBuffer.wrap(sourceString.getBytes());
		}
		return object;
	}

	@Override
	public Object decode(Object object) {
		if(object instanceof ByteBuffer){
			return MessageLoader.byteBufferToString(TObject.cast(object));
		}
		return object;
	}
}
