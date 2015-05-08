package org.voovan.network.filter;

import java.nio.ByteBuffer;

import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.network.MessageLoader;
import org.voovan.tools.TObject;

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

	@Override
	public Object encode(IoSession session,Object object) {
		if(object instanceof String){
			String sourceString = TObject.cast(object);
			return ByteBuffer.wrap(sourceString.getBytes());
		}
		return object;
	}

	@Override
	public Object decode(IoSession session,Object object) {
		if(object instanceof ByteBuffer){
			return MessageLoader.byteBufferToString((ByteBuffer)object);
		}
		return object;
	}
}
