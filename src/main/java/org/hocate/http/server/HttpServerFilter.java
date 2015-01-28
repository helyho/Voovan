package org.hocate.http.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.hocate.http.message.HttpParser;
import org.hocate.http.message.Request;
import org.hocate.http.message.Response;
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
	public synchronized Object encode(Object object) {
		if(object instanceof Response){
			Response response = TObject.cast(object);
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
	public synchronized Object decode(Object object) {
		try {
			if(object instanceof ByteBuffer){
				ByteBuffer byteBuffer = TObject.cast(object);
				ByteArrayInputStream requestInputStream = new ByteArrayInputStream(byteBuffer.array());
				Request request = HttpParser.parseRequest(requestInputStream);
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
