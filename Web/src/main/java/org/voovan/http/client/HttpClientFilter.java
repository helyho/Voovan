package org.voovan.http.client;

import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.WebServerHandler;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.network.exception.IoFilterException;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * HTTP 请求过滤器
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpClientFilter implements IoFilter {

	private final ByteBuffer emptyByteBuffer = ByteBuffer.allocateDirect(0);
	private HttpClient httpClient;

	public HttpClientFilter(HttpClient httpClient){
		this.httpClient = httpClient;
	}

	@Override
	public Object encode(IoSession session, Object object) {
		if(object instanceof WebSocketFrame){
			return ((WebSocketFrame)object).toByteBuffer();
		}
		if(object instanceof Request){
			Request request = (Request)object;
			try {
				request.send(session);
			} catch (IOException e) {
				Logger.error(e);
			}
			return emptyByteBuffer;
		}
		return null;
	}

	@Override
	public Object decode(IoSession session,Object object) throws IoFilterException {
		try{
			if(object instanceof ByteBuffer){
				ByteBuffer byteBuffer = (ByteBuffer)object;

				if(byteBuffer.limit()==0){
					session.enabledMessageSpliter(false);
				}

				ByteBufferChannel byteBufferChannel = session.getByteBufferChannel();
				if("WebSocket".equals(WebServerHandler.getAttribute(session, WebServerHandler.SessionParam.TYPE))){
					return WebSocketFrame.parse((ByteBuffer)object);
				}else {
					Response response = HttpParser.parseResponse(byteBufferChannel, session.socketContext().getReadTimeout());
					if(response.protocol().getStatus() == 101 &&
							response.header().get("Sec-WebSocket-Accept").equals("F2D56gI8wPj3dJw+vgY0KFJEtIM=")){

						//初始化 WebSocket
						httpClient.initWebSocket();

					}
					return response;
				}
			}
		}catch(IOException e){
			throw new IoFilterException("HttpClientFilter decode Error. "+e.getMessage(),e);
		}finally {
			session.enabledMessageSpliter(true);
		}
		return null;
	}
}
