package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.HttpSessionParam;
import org.voovan.http.HttpRequestType;
import org.voovan.http.message.HttpParser;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.exception.RequestTooLarge;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.network.aio.AioSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
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
	private static ThreadLocal<ByteBufferChannel> THREAD_BUFFER_CHANNEL = ThreadLocal.withInitial(()->new ByteBufferChannel(TByteBuffer.EMPTY_BYTE_BUFFER));
	private static ConcurrentSkipListMap<Integer, Request> REQUEST_CACHE = new ConcurrentSkipListMap<Integer, Request>();

	static {
		Global.getHashWheelTimer().addTask(new HashWheelTask() {
			@Override
			public void run() {
				REQUEST_CACHE.clear();
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

			try {
                httpResponse.send();
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
		} else {
			//兼容 http 的 pipeline 模式,  GET 请求直接返回指定的长度
			byteBufferChannel = THREAD_BUFFER_CHANNEL.get();
			byteBufferChannel.init(byteBuffer);
		}

		if (HttpRequestType.HTTP.equals(WebServerHandler.getAttribute(session, HttpSessionParam.TYPE))) {

			Request request = null;
			try {
				if (object instanceof ByteBuffer) {
					int hashcode = -1;
					int bodyMarkIndex = -1;
					//获取请求缓存
					if(WebContext.isRequestCache()) {
						byteBufferChannel.getByteBuffer();
						bodyMarkIndex = byteBufferChannel.indexOf(HttpStatic.BODY_MARK.getBytes()) + 4;
						hashcode = byteBufferChannel.slice(bodyMarkIndex).hashCode();
						byteBufferChannel.compact();

						//缓存控制
						if (hashcode != -1) {
							request = REQUEST_CACHE.get(hashcode);
						}
					}

					if(request==null) {
						request = HttpParser.parseRequest(byteBufferChannel, session.socketContext().getReadTimeout(), WebContext.getWebServerConfig().getMaxRequestSize());
						//增加请求缓存
						if (WebContext.isRequestCache()) {
							if (request.protocol().getMethod().equals("GET")
									&& !request.header().contain(HttpStatic.COOKIE_STRING)) {
								REQUEST_CACHE.put(hashcode, request);
								HttpParser.THREAD_REQUEST.set(new Request());
							}
						}
					} else {
						//清理缓冲区
						byteBufferChannel.shrink(bodyMarkIndex);
					}

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
