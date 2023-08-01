package org.voovan.http.websocket.filter;

import org.voovan.http.websocket.WebSocketFilter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.json.JSON;

import java.lang.reflect.Array;
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
public class StringFilter implements WebSocketFilter {

	@Override
	public Object encode(WebSocketSession session, Object object) {
		String result;
		if(object == null) {
		    result = null;
        } else if(object instanceof String){
			result = (String)object;
		} else {
			result = JSON.toJSON(object);
		}
		return result==null ? null : ByteBuffer.wrap(result.getBytes());
	}

	@Override
	public Object decode(WebSocketSession session,Object object) {
		if(object instanceof ByteBuffer){
            return TByteBuffer.toString((ByteBuffer)object);
		} else if(object.getClass().isArray() && object.getClass().getComponentType().equals(byte.class)) {
			return new String((byte[])object);
		}
		return object;
	}
}
