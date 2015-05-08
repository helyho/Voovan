package org.voovan.http.server;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.websocket.WebSocketDispatcher;
import org.voovan.http.server.websocket.WebSocketFrame;
import org.voovan.http.server.websocket.WebSocketTools;
import org.voovan.http.server.websocket.WebSocketDispatcher.WebSocketEvent;
import org.voovan.http.server.websocket.WebSocketFrame.Opcode;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

/**
 * HttpServer Socket 事件处理类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpServerHandler implements IoHandler {
	private HttpDispatcher		httpDispatcher;
	private WebSocketDispatcher	webSocketDispatcher;
	private WebServerConfig		config;

	public HttpServerHandler(WebServerConfig config, HttpDispatcher httpDispatcher, WebSocketDispatcher webSocketDispatcher) {
		this.httpDispatcher = httpDispatcher;
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
			response.setCompress(config.isGzip());

			// 构造 Http 请求响应对象
			HttpRequest httpRequest = new HttpRequest(request, defaultCharacterSet);
			HttpResponse httpResponse = new HttpResponse(response, defaultCharacterSet);

			// 填充远程连接的IP 地址和端口
			httpRequest.setRemoteAddres(session.remoteAddress());
			httpRequest.setRemotePort(session.remotePort());

			// WebSocket协议升级处理
			if (WebSocketTools.isWebSocketUpgrade(request)) {
				return disposeProtocolUpgrade(session, httpRequest, httpResponse);
			}
			// Http 1.1处理
			else {
				return disposeHttp(session, httpRequest, httpResponse);
			}
		} else if (obj instanceof WebSocketFrame) {
			return disposeWebSocket(session, TObject.cast(obj));
		}
		
		// 如果协议判断失败关闭连接
		session.close();
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
	public HttpResponse disposeHttp(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		
		// 处理响应请求
		httpDispatcher.processRoute(httpRequest, httpResponse);
		
		//如果是长连接则填充响应报文
		if (httpRequest.header().contain("Connection") 
				&& httpRequest.header().get("Connection").toLowerCase().contains("keep-alive")) {
			session.setAttribute("isKeepAlive", true);
			httpResponse.header().put("Connection", httpRequest.header().get("Connection"));
		}
		
		return httpResponse;
	}

	/**
	 * Http协议升级处理
	 * 
	 * @param session
	 * @param httpRequest
	 * @param httpResponse
	 * @return
	 */
	public HttpResponse disposeProtocolUpgrade(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		
		//保存必要参数
		session.setAttribute("isKeepAlive", true);
		
		session.setAttribute("upgradeRequest", httpRequest);

		//初始化响应消息
		httpResponse.protocol().setStatus(101);
		httpResponse.protocol().setStatusCode("Switching Protocols");
		httpResponse.header().put("Connection", "Upgrade");
		
		if(httpRequest.header().get("Upgrade").equalsIgnoreCase("websocket")){
			session.setAttribute("isWebSocket", true);
			httpResponse.header().put("Upgrade", "websocket");
			String webSocketKey = WebSocketTools.generateSecKey(httpRequest.header().get("Sec-WebSocket-Key"));
			httpResponse.header().put("Sec-WebSocket-Accept", webSocketKey);
			
			// WS_CONNECT WebSocket Open事件
			webSocketDispatcher.processRoute(WebSocketEvent.OPEN, httpRequest, null);
		}
		
		if(httpRequest.header().get("Upgrade").equalsIgnoreCase("h2c")){
			session.setAttribute("isH2c", true);
			httpResponse.header().put("Upgrade", "h2c");
			//这里写 HTTP2的实现,暂时流空
		}
		return httpResponse;
	}

	/**
	 * WebSocket 帧处理
	 * 
	 * @param session
	 * @param webSocketFrame
	 * @return
	 */
	public WebSocketFrame disposeWebSocket(IoSession session, WebSocketFrame webSocketFrame) {
		session.setAttribute("isKeepAlive", true);
		session.setAttribute("WebSocketClose", false);
		
		HttpRequest upgradeRequest = TObject.cast(session.getAttribute("upgradeRequest"));
		
		// WS_CLOSE 如果收到关闭帧则关闭连接
		if (webSocketFrame.getOpcode() == Opcode.CLOSING) {
			// WebSocket Close事件
			webSocketDispatcher.processRoute(WebSocketEvent.CLOSE, upgradeRequest, null);
			session.setAttribute("WebSocketClose", true);
			return WebSocketFrame.newInstance(true, Opcode.CLOSING, false, webSocketFrame.getFrameData());
		}
		// WS_PING 收到 ping 帧则返回 pong 帧
		else if (webSocketFrame.getOpcode() == Opcode.PING) {
			return WebSocketFrame.newInstance(true, Opcode.PONG, false, null);
		}
		// WS_RECIVE 文本和二进制消息出发 Recived 事件
		else if (webSocketFrame.getOpcode() == Opcode.TEXT || webSocketFrame.getOpcode() == Opcode.BINARY) {
			WebSocketFrame respWebSocketFrame = null;
			
			//判断解包是否有错
			if(webSocketFrame.getErrorCode()==0){
				respWebSocketFrame = webSocketDispatcher.processRoute(WebSocketEvent.RECIVED, upgradeRequest, webSocketFrame);
			}else{
				//解析时出现异常,返回关闭消息
				respWebSocketFrame = WebSocketFrame.newInstance(true, Opcode.CLOSING, false, ByteBuffer.wrap(WebSocketTools.intToByteArray(webSocketFrame.getErrorCode(), 2)));
			}
			return respWebSocketFrame;
		}

		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		if(session.containAttribute("WebSocketClose") && (boolean) session.getAttribute("WebSocketClose")){
			session.close();
		}else if (session.containAttribute("isKeepAlive") && (boolean) session.getAttribute("isKeepAlive")) {
			keepLiveSchedule(session);
		}else {
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
						webSocketDispatcher.processRoute(WebSocketEvent.CLOSE, 
								TObject.cast(session.getAttribute("upgradeRequest")), null);
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
