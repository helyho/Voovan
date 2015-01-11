package org.hocate.http.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.hocate.http.HttpPacketParser;
import org.hocate.http.message.HttpRequest;
import org.hocate.http.message.HttpResponse;
import org.hocate.network.IoFilter;
import org.hocate.tools.TObject;

/**
 * HttpServer 过滤器对象
 * @author helyho
 *
 */
public class HttpServerFilter implements IoFilter {

	/**
	 * 将HttpResponse转换成ByteBuffer
	 */
	@Override
	public Object encode(Object object) {
		if(object instanceof HttpResponse){
			HttpResponse response = TObject.cast(object);
			return ByteBuffer.wrap(response.asBytes());
		}
		else {
			return object;
		}
		
	}

	/**
	 * 将请求ByteBuffer转换成 HttpRequest
	 */
	@Override
	public Object decode(Object object) {
		try {
			if(object instanceof ByteBuffer){
				ByteBuffer byteBuffer = TObject.cast(object);
				ByteArrayInputStream requestInputStream = new ByteArrayInputStream(byteBuffer.array());
				HttpRequest request = HttpPacketParser.parseRequest(requestInputStream);
				return request;
			}else{
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
