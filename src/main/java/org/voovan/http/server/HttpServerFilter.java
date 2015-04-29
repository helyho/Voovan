package org.voovan.http.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.websocket.WebSocketFrame;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

/**
 * HttpServer 过滤器对象
 * 
 * @author helyho
 *
 */
public class HttpServerFilter implements IoFilter {

	private static final String IS_WEB_SOCKET="isWebSocket";
	
	/**
	 * 将HttpResponse转换成ByteBuffer
	 */
	@Override
	public synchronized Object encode(IoSession session, Object object) {
		// 对 Websocket 进行处理
		if (object instanceof Response) {
			Response response = TObject.cast(object);
			return ByteBuffer.wrap(response.asBytes());
		} else if(object instanceof WebSocketFrame){
			WebSocketFrame webSocketFrame = TObject.cast(object);
			return webSocketFrame.toByteBuffer();
		}
		return null;
	}

	/**
	 * 将请求ByteBuffer转换成 HttpRequest
	 */
	@Override
	public synchronized Object decode(IoSession session, Object object) {
		//如果 Session 中不包含isWebSocket,则是 Http 请求,转换成 Http 的 Request 请求
		if (session.getAttribute(IS_WEB_SOCKET)==null) {
			try {
				if (object instanceof ByteBuffer) {
					ByteBuffer byteBuffer = TObject.cast(object);
					ByteArrayInputStream requestInputStream = new ByteArrayInputStream(byteBuffer.array());
					Request request = HttpParser.parseRequest(requestInputStream);
					if(request!=null){
						return request;
					}else{
						session.close();
					}
				} else {
					return null;
				}
			} catch (IOException e) {
				Logger.error("Class HttpServerFilter Error: "+e.getMessage());
				e.printStackTrace();
				return null;
			}
		
		}
		//如果包含isWebSocket,且为 true 曾是 WebSocket,转换成 WebSocketFrame 对象
		else if((boolean)session.getAttribute(IS_WEB_SOCKET)){
			if (object instanceof ByteBuffer) {
				ByteBuffer byteBuffer = TObject.cast(object);
				return WebSocketFrame.parse(byteBuffer);
			} else {
				return null;
			}
		}
		return null;
	}

}
