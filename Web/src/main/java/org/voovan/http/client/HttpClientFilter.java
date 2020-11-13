package org.voovan.http.client;

import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Response;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.WebServerHandler;
import org.voovan.http.message.exception.HttpParserException;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.network.exception.IoFilterException;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.buffer.TByteBuffer;
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
	private HttpClient httpClient;

	public HttpClientFilter(HttpClient httpClient){
		this.httpClient = httpClient;
	}

	@Override
	public Object encode(IoSession session, Object object) {
		if(object instanceof WebSocketFrame){
			return ((WebSocketFrame)object).toByteBuffer();
		}
		if(object instanceof HttpRequest){
			HttpRequest httpRequest = (HttpRequest)object;
			try {
				httpRequest.send();
			} catch (IOException e) {
				Logger.error(e);
			}
			return TByteBuffer.EMPTY_BYTE_BUFFER;
		}
		return null;
	}

	@Override
	public Object decode(IoSession session,Object object) throws IoFilterException {
		ByteBufferChannel byteBufferChannel = session.getReadByteBufferChannel();

		try{
			if(object instanceof ByteBuffer){
				ByteBuffer byteBuffer = (ByteBuffer)object;

				if(WebServerHandler.getSessionState(session).isWebSocket()){
					return WebSocketFrame.parse((ByteBuffer)object);
				}else {
					Response response = (Response) ((Object[])session.getAttachment())[3];
					response = HttpParser.parseResponse(response, session, byteBufferChannel, session.socketContext().getReadTimeout());

					return response;
				}
			}
		}catch(Exception e) {
			byteBufferChannel.clear();

			if(e instanceof HttpParserException) {
				session.close();
			}

			throw new IoFilterException("HttpClientFilter decode Error. "+e.getMessage(),e);
		}
		return null;
	}
}
