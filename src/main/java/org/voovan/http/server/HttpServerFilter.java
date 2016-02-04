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
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpServerFilter implements IoFilter {

	
	/**
	 * 将HttpResponse转换成ByteBuffer
	 */
	@Override
	public Object encode(IoSession session, Object object) {
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
	public Object decode(IoSession session, Object object) {
		ByteBuffer byteBuffer = TObject.cast(object);
		if (isHttpRequest(byteBuffer)) {
			try {
				if (object instanceof ByteBuffer) {
					
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
				Logger.error("ParseRequest failed.",e);
				return null;
			}
		
		}
		//如果包含Type为 WebSocket 说明是 WebSocket 通信,转换成 WebSocketFrame 对象
		else if("WebSocket".equals(session.getAttribute("Type"))){
			if (object instanceof ByteBuffer) {
				WebSocketFrame webSocketFrame = WebSocketFrame.parse(byteBuffer);
				if(webSocketFrame.getErrorCode()==0){
					return webSocketFrame;
				}else{
					session.close();
				}
			} else {
				return null;
			}
		}
		//如果协议判断失败关闭连接
		session.close();
		return null;
	}

	/**
	 * 判断是否是 Http 请求
	 * @param byteBuffer
	 * @return
	 */
	public static boolean isHttpRequest(ByteBuffer byteBuffer){
		int loadSize = (byteBuffer.limit() > 7 ? 7 : byteBuffer.limit());
		if (loadSize < 7) {
			return false;
		} else {
			byte[] testBytes = new byte[loadSize];
			for (int i = 0; i < loadSize; i++) {
				byte singleByte = byteBuffer.get(i);
				// 大写字母
				if (singleByte < 91 && singleByte > 64) {
					testBytes[i] = singleByte;
				}
				//空格退出
				else if (singleByte == 32) {
					break;
				} else {
					return false;
				}

			}
			String testStr = new String(testBytes).trim();
			String httpMethod = testStr.split(" ")[0];
			if ("GET".equals(httpMethod) 
					|| "POST".equals(httpMethod) 
					|| "HEAD".equals(httpMethod) 
					|| "PUT".equals(httpMethod) 
					|| "DELETE".equals(httpMethod)  
					|| "TRACE".equals(httpMethod) 
					|| "CONNECT".equals(httpMethod) 
					|| "OPTIONS".equals(httpMethod) ) {
				return true;
			} else {
				return false;
			}
		}
	}
}
