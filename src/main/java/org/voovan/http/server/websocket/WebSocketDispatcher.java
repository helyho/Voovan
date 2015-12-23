package org.voovan.http.server.websocket;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.voovan.http.server.HttpDispatcher;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.server.websocket.WebSocketFrame.Opcode;
import org.voovan.network.IoSession;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

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

	public enum WebSocketEvent {
		OPEN, RECIVED, SENT, CLOSE
	}

	/**
	 * 构造函数
	 * 
	 * @param rootDir
	 *            根目录
	 */
	public WebSocketDispatcher(WebServerConfig config) {
		handlers = new HashMap<String, WebSocketBizHandler>();
	}

	/**
	 * 增加一个路由规则
	 * 
	 * @param Method
	 * @param routeRegexPath
	 * @param routeBuiz
	 */
	public void addRouteHandler(String routeRegexPath, WebSocketBizHandler handler) {
		handlers.put(routeRegexPath, handler);
	}

	/**
	 * 路由处理函数
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
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
				try {
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
				} catch (UnsupportedEncodingException e) {
					Logger.error(e);
				}
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
		if (session.containAttribute("isWebSocket") && (boolean) session.getAttribute("isWebSocket") &&
				session.containAttribute("WebSocketClose") && (boolean) session.getAttribute("WebSocketClose") &&
				!session.close()
					) {
				// 触发一个 WebSocket Close 事件
				processRoute(WebSocketEvent.CLOSE, 
						TObject.cast(session.getAttribute("upgradeRequest")), null);
			}
	}
}
