package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.HttpRequestType;
import org.voovan.http.HttpSessionParam;
import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.exception.HttpParserException;
import org.voovan.http.server.exception.RequestTooLarge;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebServer 过滤器对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebServerFilter implements IoFilter {
	public static ConcurrentHashMap<Long, byte[]> RESPONSE_CACHE = new ConcurrentHashMap<Long, byte[]>();

	static {
		Global.getHashWheelTimer().addTask(new HashWheelTask() {
			@Override
			public void run() {
				RESPONSE_CACHE.clear();
			}
		}, 1);
	}

	/**
	 * 将HttpResponse转换成ByteBuffer
	 */
	@Override
	public Object encode(IoSession session, Object object) {
		// 对 Websocket 进行处理
		if (object instanceof HttpResponse) {
			HttpResponse httpResponse = (HttpResponse)object;

			try{
				if(!httpResponse.isAsync()) {
					Long mark = httpResponse.getMark();
                    if (WebContext.isCache() && mark!=null) {
						byte[] cacheBytes = RESPONSE_CACHE.get(mark);

                        if (cacheBytes == null) {
                            ByteBufferChannel sendByteBufferChannel = session.getSendByteBufferChannel();
                            int size = sendByteBufferChannel.size();
                            httpResponse.send();

                            if (size == 0) {
                                cacheBytes = new byte[session.getSendByteBufferChannel().size()];
                                sendByteBufferChannel.get(cacheBytes);
                                RESPONSE_CACHE.putIfAbsent(mark, cacheBytes);
                            }
                        } else {
                            session.send(ByteBuffer.wrap(cacheBytes));
                        }
                    } else {
                        httpResponse.send();
                    }
				} else {
					//异步响应返回 null, socket 不会在同步模式发送响应
					return null;
				}
			} catch(Exception e){
				Logger.error(e);
			} finally {
				httpResponse.clear();
			}

			return TByteBuffer.EMPTY_BYTE_BUFFER;
		} else if(object instanceof WebSocketFrame){
			WebSocketFrame webSocketFrame = (WebSocketFrame)object;
			return webSocketFrame.toByteBuffer();
		}
		return null;
	}

	/**
	 * 将请求ByteBuffer转换成 HttpRequest
	 */
	@Override
	public Object decode(IoSession session, Object object) {
		if(!session.isConnected()){
			return null;
		}

		if (HttpRequestType.HTTP.equals(WebServerHandler.getAttribute(session, HttpSessionParam.TYPE))) {

			ByteBufferChannel byteBufferChannel = byteBufferChannel = session.getReadByteBufferChannel();

			try {
				Request request = HttpParser.parseRequest(session, byteBufferChannel, session.socketContext().getReadTimeout(), WebContext.getWebServerConfig().getMaxRequestSize());
				if(request!=null){
					return request;
				}else{
					session.close();
				}
			} catch (Exception e) {
				byteBufferChannel.clear();

				if(e instanceof HttpParserException) {
					session.close();
				}

				Response response = new Response();
				response.protocol().setStatus(500);

				//如果请求过大的异常处理
				if(e instanceof RequestTooLarge){
					response.protocol().setStatus(413);
					response.body().write("false");
				}

				try {
					response.send(session);
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				Logger.error("ParseRequest failed",e);
				return null;
			}
		}
		//如果包含Type为 WebSocket 说明是 WebSocket 通信,转换成 WebSocketFrame 对象
		else if(HttpRequestType.WEBSOCKET.equals(WebServerHandler.getAttribute(session, HttpSessionParam.TYPE))){

			ByteBuffer byteBuffer = (ByteBuffer)object;

			if (object instanceof ByteBuffer && byteBuffer.limit()!=0) {
				WebSocketFrame webSocketFrame = WebSocketFrame.parse(byteBuffer);

				if(webSocketFrame.getErrorCode()==0){
					return webSocketFrame;
				}else{
					session.close();
				}
			} else {
				return null;
			}
		} else {
			//如果协议判断失败关闭连接
			session.close();
		}
		return null;
	}
}
