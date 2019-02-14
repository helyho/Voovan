package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.HttpSessionParam;
import org.voovan.http.HttpRequestType;
import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.exception.RequestTooLarge;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.network.aio.AioSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentSkipListMap;

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
	public static ConcurrentSkipListMap<Long, byte[]> RESPONSE_CACHE = new ConcurrentSkipListMap<Long, byte[]>();

	static {
		Global.getHashWheelTimer().addTask(new HashWheelTask() {
			@Override
			public void run() {
				RESPONSE_CACHE.clear();
			}
		}, 1000);
	}

	/**
	 * 将HttpResponse转换成ByteBuffer
	 */
	@Override
	public Object encode(IoSession session, Object object) {
		session.enabledMessageSpliter(true);

		// 对 Websocket 进行处理
		if (object instanceof HttpResponse) {
			HttpResponse httpResponse = (HttpResponse)object;

			try{
				if(httpResponse.isAutoSend()) {
					byte[] cacheBytes = null;
					long mark = httpResponse.getMark();

					if(WebContext.isCache()) {
						cacheBytes = RESPONSE_CACHE.get(mark);
					}

					if(cacheBytes==null) {
						httpResponse.send();
						if(mark!=0) {
							ByteBufferChannel sendByteBufferChannel = session.getSendByteBufferChannel();
							cacheBytes = new byte[session.getSendByteBufferChannel().size()];
							sendByteBufferChannel.get(cacheBytes);
							RESPONSE_CACHE.put(mark, cacheBytes);
						}
					} else {
						session.sendByBuffer(ByteBuffer.wrap(cacheBytes));
						httpResponse.clear();
					}
				}
			}catch(Exception e){
				Logger.error(e);
			}

			return null;
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

		ByteBuffer byteBuffer = (ByteBuffer)object;

		ByteBufferChannel byteBufferChannel = null;

		if(byteBuffer.limit()==0){
			session.enabledMessageSpliter(false);
			byteBufferChannel = session.getReadByteBufferChannel();
		}

		if (HttpRequestType.HTTP.equals(WebServerHandler.getAttribute(session, HttpSessionParam.TYPE))) {

			Request request = null;
			try {
				if (object instanceof ByteBuffer) {
					request = HttpParser.parseRequest(byteBufferChannel, session.socketContext().getReadTimeout(), WebContext.getWebServerConfig().getMaxRequestSize());

					if(request!=null){
						return request;
					}else{
						session.close();
					}

				} else {
					return null;
				}
			} catch (IOException e) {
				Response response = new Response();
				response.protocol().setStatus(500);

				//如果请求过大的异常处理
				if(e instanceof RequestTooLarge){
					response.protocol().setStatus(413);
					response.body().write("false");
				}

				try {
					response.send(session);
					((AioSocket)session.socketContext()).socketChannel().shutdownInput();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				session.close();

				Logger.error("ParseRequest failed",e);
				return null;
			}
		}
		//如果包含Type为 WebSocket 说明是 WebSocket 通信,转换成 WebSocketFrame 对象
		else if(HttpRequestType.WEBSOCKET.equals(WebServerHandler.getAttribute(session, HttpSessionParam.TYPE))){
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
