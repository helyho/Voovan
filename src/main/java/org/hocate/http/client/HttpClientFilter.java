package org.hocate.http.client;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

import org.hocate.http.message.HttpParser;
import org.hocate.http.message.Request;
import org.hocate.http.message.Response;
import org.hocate.network.IoFilter;
import org.hocate.tools.TObject;

public class HttpClientFilter implements IoFilter {

	@Override
	public Object encode(Object object) {
		
		if(object instanceof Request){
			Request request = TObject.cast(object);
			return ByteBuffer.wrap(request.asBytes());
		}
		return null;
	}

	@Override
	public Object decode(Object object) {
		try{
			if(object instanceof ByteBuffer){
				ByteBuffer byteBuffer = TObject.cast(object);
				Response response = HttpParser.parseResponse(new ByteArrayInputStream(byteBuffer.array()));
				return response;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
