package org.voovan.http.client;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

public class HttpClientFilter implements IoFilter {

	@Override
	public Object encode(IoSession session,Object object) {
		
		if(object instanceof Request){
			Request request = TObject.cast(object);
			return ByteBuffer.wrap(request.asBytes());
		}
		return null;
	}

	@Override
	public Object decode(IoSession session,Object object) {
		try{
			if(object instanceof ByteBuffer){
				ByteBuffer byteBuffer = TObject.cast(object);
				Response response = HttpParser.parseResponse(new ByteArrayInputStream(byteBuffer.array()));
				return response;
			}
		}catch(Exception e){
			Logger.error("Class HttpClientFilter decode Error: "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}
