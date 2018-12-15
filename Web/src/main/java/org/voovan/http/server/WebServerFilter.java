package org.voovan.http.server;

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
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.ThreadLocalPool;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

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
	private static ThreadLocalPool<ByteBufferChannel> THREAD_LOCAL_MULIT_BUFFER_CHANNEL = new ThreadLocalPool<ByteBufferChannel>();

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
				synchronized (session) {
					httpResponse.send();
				}
			}catch(Exception e){
				Logger.error(e);
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

		boolean needRelease = false;

		ByteBuffer byteBuffer = (ByteBuffer)object;

		ByteBufferChannel byteBufferChannel = null;

		if(byteBuffer.limit()==0){
			session.enabledMessageSpliter(false);
			byteBufferChannel = session.getByteBufferChannel();
		} else {
			//兼容 http 的 pipeline 模式,  GET 请求直接返回指定的长度
			byteBufferChannel = THREAD_LOCAL_MULIT_BUFFER_CHANNEL.get(()->new ByteBufferChannel(TByteBuffer.EMPTY_BYTE_BUFFER));
			byteBufferChannel.init(byteBuffer);
			needRelease = true;
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
			} finally {
				if(needRelease) {
					ByteBufferChannel finalByteBufferChannel = byteBufferChannel;
					THREAD_LOCAL_MULIT_BUFFER_CHANNEL.release(byteBufferChannel, ()->{
						finalByteBufferChannel.release();
						return false;
					});
				}
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
