package org.voovan.http.server.websocket;

import org.voovan.http.server.HttpDispatcher;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.server.websocket.WebSocketFrame.Opcode;
import org.voovan.network.IoSession;
import org.voovan.tools.TObject;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 根据 WebSocket 请求分派到处理路由
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebSocketDispatcher {
	/**
	 * [Key] = Route path ,[Value] = WebSocketBizHandler对象
	 */
	private Map<String, WebSocketBizHandler>	handlers;

	private static final String IS_WEB_SOCKET = "isWebSocket";
	private static final String WEB_SOCKET_ClOSE = "WebSocketClose";
	
	public enum WebSocketEvent {
		OPEN, RECIVED, SENT, CLOSE
	}

	/**
	 * 构造函数
	 * 
	 * @param config
	 *            根目录
	 */
	public WebSocketDispatcher(WebServerConfig config) {
		handlers = new HashMap<String, WebSocketBizHandler>();
	}

	/**
	 * 增加一个路由规则
	 * 
	 * @param routeRegexPath
	 * @param handler
	 */
	public void addRouteHandler(String routeRegexPath, WebSocketBizHandler handler) {
		handlers.put(routeRegexPath, handler);
	}

	/**
	 * 路由处理函数
	 * 
	 * @param event
	 * @param request
	 * @param webSocketFrame
	 */
	public WebSocketFrame processRoute(WebSocketEvent event, HttpRequest request, WebSocketFrame webSocketFrame) {
		
		String requestPath = request.protocol().getPath();

		boolean isMatched = false;
		for (String routePath : handlers.keySet()) {
			// 路由匹配
			isMatched = HttpDispatcher.matchPath(requestPath, routePath);
			if (isMatched) {
				// 获取路由处理对象
				WebSocketBizHandler handler = handlers.get(routePath);
				
				// 获取路径变量
				ByteBuffer responseMessage = null;
				Map<String, String> variables = HttpDispatcher.fetchPathVariables(requestPath, routePath);
				request.getParameters().putAll(variables);
				
				//WebSocket 事件处理
				if (event == WebSocketEvent.OPEN) {
				    handler.onOpen(request);
				} else if (event == WebSocketEvent.RECIVED) {
					responseMessage = handler.onRecived(request, webSocketFrame.getFrameData());
				} else if (event == WebSocketEvent.CLOSE) {
					handler.onClose();
				}
				
				//将返回消息包装称WebSocketFrame
				if (responseMessage != null) {
					return WebSocketFrame.newInstance(true, Opcode.TEXT, false, responseMessage);
				}
				break;
		
			}
		}

		// 没有找寻到匹配的路由处理器
		if (!isMatched) {
			new RouterNotFound("Not avaliable router!").printStackTrace();
		}
		return null;
	}
	
	/**
	 * 出发 Close 事件
	 * @param session
	 */
	public void fireCloseEvent(IoSession session){
		//检查是否是WebSocket
		if (session.containAttribute(IS_WEB_SOCKET) && (boolean) session.getAttribute(IS_WEB_SOCKET) &&
				session.containAttribute(WEB_SOCKET_ClOSE) && (boolean) session.getAttribute(WEB_SOCKET_ClOSE) &&
				!session.close()
					) {
				// 触发一个 WebSocket Close 事件
				processRoute(WebSocketEvent.CLOSE, 
						TObject.cast(session.getAttribute("upgradeRequest")), null);
			}
	}
}
