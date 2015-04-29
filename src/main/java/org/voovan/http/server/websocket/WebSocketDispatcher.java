package org.voovan.http.server.websocket;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.voovan.http.server.HttpDispatcher;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.WebConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.server.websocket.WebSocketFrame.Opcode;

/**
 * 
 * 根据 WebSocket 请求分派到处理路由
 * 

 * @author helyho
 *
 */
public class WebSocketDispatcher {
	/**
	 * [MainKey] = HTTP method ,[Value Key] = Route path, [Value value] =
	 * RouteBuiz对象
	 */
	private Map<String, WebSocketBizHandler>	routes;

	public enum WebSocketEvent {
		OPEN, RECIVED, SENT, CLOSE
	}

	/**
	 * 构造函数
	 * 
	 * @param rootDir
	 *            根目录
	 */
	public WebSocketDispatcher(WebConfig config) {
		routes = new HashMap<String, WebSocketBizHandler>();
	}

	/**
	 * 增加一个路由规则
	 * 
	 * @param Method
	 * @param routeRegexPath
	 * @param routeBuiz
	 */
	public void addRouteHandler(String routeRegexPath, WebSocketBizHandler handler) {
		routes.put(routeRegexPath, handler);
	}

	/**
	 * 路由处理函数
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public WebSocketFrame Process(WebSocketEvent event, HttpRequest request, WebSocketFrame webSocketFrame) {
		String requestPath = request.protocol().getPath();

		boolean isMatched = false;
		for (String routePath : routes.keySet()) {
			// 路由匹配
			isMatched = HttpDispatcher.matchPath(requestPath, routePath);
			if (isMatched) {
				// 获取路由处理对象
				WebSocketBizHandler handler = routes.get(routePath);
				// 获取路径变量
				try {
					ByteBuffer responseMessage = null;
					Map<String, String> variables = HttpDispatcher.fetchPathVariables(requestPath, routePath);
					variables.putAll(request.getParameters());
					
					//WebSocket 事件处理
					if (event == WebSocketEvent.OPEN) {
					    handler.onOpen(variables);
					} else if (event == WebSocketEvent.RECIVED) {
						responseMessage = handler.onRecived(variables, webSocketFrame.getFrameData());
					} else if (event == WebSocketEvent.CLOSE) {
						handler.onClose();
					}
					
					//将返回消息包装称WebSocketFrame
					if (responseMessage != null) {
						return WebSocketFrame.newInstance(true, Opcode.TEXT, false, responseMessage);
					}
					break;
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}

		// 没有找寻到匹配的路由处理器
		if (!isMatched) {
			new RouterNotFound("Not avaliable router!").printStackTrace();
		}
		return null;
	}
}
