package org.voovan.http.server;

import org.voovan.http.message.HttpParser;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.network.IoFilter;
import org.voovan.network.IoSession;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TString;
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
	private final ByteBuffer emptyByteBuffer = ByteBuffer.allocateDirect(0);

	/**
	 * 将HttpResponse转换成ByteBuffer
	 */
	@Override
	public Object encode(IoSession session, Object object) {
		session.enabledMessageSpliter(true);

		// 对 Websocket 进行处理
		if (object instanceof Response) {
			Response response = (Response)object;
			try {
				response.send(session);
			}catch(Exception e){
				Logger.error(e);
			}
			return emptyByteBuffer;
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
		ByteBuffer byteBuffer = (ByteBuffer)object;

		if(byteBuffer.limit()==0){
			session.enabledMessageSpliter(false);
		}

		ByteBufferChannel byteBufferChannel = session.getByteBufferChannel();
		if (isHttpRequest(byteBufferChannel)) {
			try {
				if (object instanceof ByteBuffer) {
					Request request = HttpParser.parseRequest(byteBufferChannel, session.socketContext().getReadTimeout());
					if(request!=null){
						return request;
					}else{
						session.close();
					}
				} else {
					return null;
				}
			} catch (IOException e) {
				Logger.error("ParseRequest failed",e);
				return null;
			}
		}
		//如果包含Type为 WebSocket 说明是 WebSocket 通信,转换成 WebSocketFrame 对象
		else if("WebSocket".equals(WebServerHandler.getAttribute(session, WebServerHandler.SessionParam.TYPE))){
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

	/**
	 * 判断是否是 HTTP 请求
	 * @param byteBufferChannel 请求字节换缓冲对象
	 * @return  是否是 HTTP 请求
	 */
	public static boolean isHttpRequest(ByteBufferChannel byteBufferChannel) {
		String testStr = null;

		if(byteBufferChannel.isReleased()){
			return false;
		}

		int lineEndIndex = byteBufferChannel.indexOf("\n".getBytes());

		if(lineEndIndex>0) {
			byte[] tmpByte = new byte[lineEndIndex];
			byteBufferChannel.get(tmpByte);
			testStr = new String(tmpByte);

			if (testStr != null && TString.regexMatch(testStr, "HTTP.{0,4}") == 1) {
				return true;
			} else {
				return false;
			}
		}else{
			return false;
		}
	}
}
