package org.hocate.http.server;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import org.hocate.http.message.Request;
import org.hocate.http.message.Response;
import org.hocate.http.server.websocket.WebSocketTools;
import org.hocate.http.server.websocket.WebSocketFrame;
import org.hocate.http.server.websocket.WebSocketFrame.Opcode;
import org.hocate.log.Logger;
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
	private RequestDispatcher	requestDispatcher;
	private WebConfig			config;

	public HttpServerHandler(WebConfig config, RequestDispatcher requestDispatcher) {
		this.requestDispatcher = requestDispatcher;
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
		// Http 请求
		if (obj instanceof Request) {
			// 构造请求
			Request request = TObject.cast(obj);
			// 构造响应报文并返回
			Response response = new Response();

			// WebSocket协议升级处理
			if (WebSocketTools.isWebSocketRequest(request.header())) {
				session.setAttribute("isKeepAlive", true);
				session.setAttribute("isWebSocket", true);
				session.setAttribute("path", request.protocol().getPath());
				
				response.protocol().setStatus(101);
				response.protocol().setStatusCode("Switching Protocols");
				response.header().put("Upgrade", "websocket");
				response.header().put("Connection", "Upgrade");
				String webSocketKey = WebSocketTools.generateSecKey(request.header().get("Sec-WebSocket-Key"));
				response.header().put("Sec-WebSocket-Accept", webSocketKey);
				
			}
			// Http 1.1处理
			else {
				// 设置默认字符集
				String defaultCharacterSet = config.getCharacterSet();
				HttpRequest httpRequest = new HttpRequest(request, defaultCharacterSet);
				HttpResponse httpResponse = new HttpResponse(response, defaultCharacterSet);

				// 填充远程连接的IP 地址和端口
				httpRequest.setRemoteAddres(session.remoteAddress());
				httpRequest.setRemotePort(session.remotePort());

				// 处理响应请求
				requestDispatcher.Process(httpRequest, httpResponse);
				if (request.header().contain("Connection")) {
					session.setAttribute("isKeepAlive", true);
					response.header().put("Connection", request.header().get("Connection"));
				}
			}
			return response;
		} else if (obj instanceof WebSocketFrame 
				&& session.containAttribute("isKeepAlive")
				&& (boolean) session.getAttribute("isKeepAlive")) {
			session.setAttribute("isKeepAlive", true);
			WebSocketFrame webSocketFrame = TObject.cast(obj);
			
			//如果收到关闭帧则关闭连接
			if(webSocketFrame.getOpcode()==Opcode.CLOSING){
				session.close();
			}
			//收到 ping 帧则返回 pong 帧
			else if(webSocketFrame.getOpcode()==Opcode.PING){
				WebSocketFrame.newInstance(true, Opcode.PONG, false, null);
			}
			System.out.println(webSocketFrame);
			webSocketFrame.setTransfereMask(false);
			webSocketFrame.setFrameData(ByteBuffer.wrap("hello helyho".getBytes()));
			return webSocketFrame;
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
