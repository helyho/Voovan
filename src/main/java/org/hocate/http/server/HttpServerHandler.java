package org.hocate.http.server;

import java.util.Timer;
import java.util.TimerTask;

import org.hocate.http.message.Request;
import org.hocate.http.message.Response;
import org.hocate.http.server.websocket.WebSocketDispatcher;
import org.hocate.http.server.websocket.WebSocketFrame;
import org.hocate.http.server.websocket.WebSocketTools;
import org.hocate.http.server.websocket.WebSocketDispatcher.WebSocketEvent;
import org.hocate.http.server.websocket.WebSocketFrame.Opcode;
import org.hocate.tools.log.Logger;
import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.tools.TEnv;
import org.hocate.tools.TObject;

/**
 * HttpServer 业务处理类
 * 
 * @author helyho
 *
 */
public class HttpServerHandler implements IoHandler {
	private HttpDispatcher		requestDispatcher;
	private WebSocketDispatcher	webSocketDispatcher;
	private WebConfig			config;

	public HttpServerHandler(WebConfig config, HttpDispatcher requestDispatcher, WebSocketDispatcher webSocketDispatcher) {
		this.requestDispatcher = requestDispatcher;
		this.webSocketDispatcher = webSocketDispatcher;
		this.config = config;
	}

	@Override
	public Object onConnect(IoSession session) {
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {

	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		// 获取默认字符集
		String defaultCharacterSet = config.getCharacterSet();

		// Http 请求
		if (obj instanceof Request) {
			// 构造请求
			Request request = TObject.cast(obj);
			// 构造响应报文并返回
			Response response = new Response();

			// 构造 Http 请求响应对象
			HttpRequest httpRequest = new HttpRequest(request, defaultCharacterSet);
			HttpResponse httpResponse = new HttpResponse(response, defaultCharacterSet);

			// WebSocket协议升级处理
			if (WebSocketTools.isWebSocketRequest(request.header())) {
				return DisposeWebSocketUpgrade(session, httpRequest, httpResponse);
			}
			// Http 1.1处理
			else {
				return DisposeHttp(session, httpRequest, httpResponse);
			}
		} else if (obj instanceof WebSocketFrame) {
			return DisposeWebSocket(session, TObject.cast(obj));
		}
		return null;
	}

	/**
	 * Http 请求响应处理
	 * 
	 * @param session
	 * @param httpRequest
	 * @param httpResponse
	 * @return
	 */
	public HttpResponse DisposeHttp(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		// 填充远程连接的IP 地址和端口
		httpRequest.setRemoteAddres(session.remoteAddress());
		httpRequest.setRemotePort(session.remotePort());

		// 处理响应请求
		requestDispatcher.Process(httpRequest, httpResponse);
		if (httpRequest.header().contain("Connection")) {
			session.setAttribute("isKeepAlive", true);
			httpResponse.header().put("Connection", httpRequest.header().get("Connection"));
		}
		return httpResponse;
	}

	/**
	 * WebSocket 协议升级处理
	 * 
	 * @param session
	 * @param httpRequest
	 * @param httpResponse
	 * @return
	 */
	public HttpResponse DisposeWebSocketUpgrade(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		session.setAttribute("isKeepAlive", true);
		session.setAttribute("isWebSocket", true);
		session.setAttribute("WebSocketRequest", httpRequest);

		httpResponse.protocol().setStatus(101);
		httpResponse.protocol().setStatusCode("Switching Protocols");
		httpResponse.header().put("Upgrade", "websocket");
		httpResponse.header().put("Connection", "Upgrade");
		String webSocketKey = WebSocketTools.generateSecKey(httpRequest.header().get("Sec-WebSocket-Key"));
		httpResponse.header().put("Sec-WebSocket-Accept", webSocketKey);

		// WebSocket Open事件
		webSocketDispatcher.Process(WebSocketEvent.OPEN, httpRequest, null);
		return httpResponse;
	}

	/**
	 * WebSocket 帧处理
	 * 
	 * @param session
	 * @param webSocketFrame
	 * @return
	 */
	public WebSocketFrame DisposeWebSocket(IoSession session, WebSocketFrame webSocketFrame) {
		session.setAttribute("isKeepAlive", true);

		// 如果收到关闭帧则关闭连接
		if (webSocketFrame.getOpcode() == Opcode.CLOSING) {
			session.close();
			// WebSocket Close事件
			webSocketDispatcher.Process(WebSocketEvent.CLOSE, TObject.cast(session.getAttribute("WebSocketRequest")), null);
		}
		// 收到 ping 帧则返回 pong 帧
		else if (webSocketFrame.getOpcode() == Opcode.PING) {
			WebSocketFrame.newInstance(true, Opcode.PONG, false, null);
		}
		// 文本和二进制消息出发 Recived 事件
		else if (webSocketFrame.getOpcode() == Opcode.TEXT || webSocketFrame.getOpcode() == Opcode.BINARY) {
			WebSocketFrame responseWebSocketFrame = webSocketDispatcher.Process(WebSocketEvent.RECIVED,
					TObject.cast(session.getAttribute("WebSocketRequest")), webSocketFrame);
			return responseWebSocketFrame;
		}

		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		if (session.containAttribute("isKeepAlive") && (boolean) session.getAttribute("isKeepAlive")) {
			keepLiveSchedule(session);
		} else {
			session.close();
		}
	}

	@Override
	public void onException(IoSession session, Exception e) {
		Logger.error("Http Server Error: \r\n" + e.getClass().getName() + "\r\n" + TEnv.getStackElementsMessage(e.getStackTrace()));
	}

	/**
	 * 通过 Session 来控制 keepAlive 超时,超时后关闭连接 同一个连接第二次发送消息,超时时间重置
	 * 
	 * @param session
	 */
	private void keepLiveSchedule(IoSession session) {
		// 取消上次的 KeepAliveTask
		if (session.getAttribute("keepAliveTimer") != null) {
			Timer oldTimer = (Timer) session.getAttribute("keepAliveTimer");
			oldTimer.cancel();
		}

		// 构造新的KeepAliveTask
		Timer keepAliveTimer = new Timer();
		int keepAliveTimeout = config.getKeepAliveTimeout();

		if (keepAliveTimeout > 0) {
			TimerTask keepAliveTask = new TimerTask() {
				@Override
				public void run() {
					// 如果是 WebSocket 则出发 Close 事件
					if (session.containAttribute("isWebSocket") && (boolean) session.getAttribute("isWebSocket")) {
						// WebSocket Close事件
						webSocketDispatcher
								.Process(WebSocketEvent.CLOSE, TObject.cast(session.getAttribute("WebSocketRequest")), null);
					}
					session.close();
				}
			};
			keepAliveTimer.schedule(keepAliveTask, keepAliveTimeout * 60 * 1000);
			session.setAttribute("keepAliveTimer", keepAliveTimer);
		} else {
			session.close();
		}

	}
}
