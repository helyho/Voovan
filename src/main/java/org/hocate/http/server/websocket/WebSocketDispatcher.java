package org.hocate.http.server.websocket;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.hocate.http.server.HttpDispatcher;
import org.hocate.http.server.HttpRequest;
import org.hocate.http.server.WebConfig;
import org.hocate.http.server.exception.RouterNotFound;
import org.hocate.http.server.websocket.WebSocketFrame.Opcode;

/**
 * 
 * 根据 Request 请求分派到处理路由
 * 
 * 
 * GET 请求获取Request-URI所标识的资源<br/>
 * POST 在Request-URI所标识的资源后附加新的数据<br/>
 * HEAD 请求获取由Request-URI所标识的资源的响应消息报头<br/>
 * PUT 请求服务器存储一个资源，并用Request-URI作为其标识<br/>
 * DELETE 请求服务器删除Request-URI所标识的资源<br/>
 * TRACE 请求服务器回送收到的请求信息，主要用于测试或诊断<br/>
 * CONNECT 保留将来使用<br/>
 * OPTIONS 请求查询服务器的性能，或者查询与资源相关的选项和需求<br/>
 * 
 * @author helyho
 *
 */
public class WebSocketDispatcher {
	/**
	 * [MainKey] = HTTP method ,[Value Key] = Route path, [Value value] =
	 * RouteBuiz对象
	 */
	private Map<String, WebSocketHandler>	routes;

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
		routes = new HashMap<String, WebSocketHandler>();
	}

	/**
	 * 增加一个路由规则
	 * 
	 * @param Method
	 * @param routeRegexPath
	 * @param routeBuiz
	 */
	public void addRouteHandler(String routeRegexPath, WebSocketHandler handler) {
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
				WebSocketHandler handler = routes.get(routePath);
				// 获取路径变量
				try {
					ByteBuffer responseMessage = null;
					Map<String, String> variables = HttpDispatcher.fetchPathVariables(requestPath, routePath);
					variables.putAll(request.getParameters());
					if (event == WebSocketEvent.OPEN) {
					    handler.onOpen(variables);
					} else if (event == WebSocketEvent.RECIVED) {
						responseMessage = handler.onRecived(variables, webSocketFrame.getFrameData());
					} else if (event == WebSocketEvent.CLOSE) {
						handler.onClose();
					}
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
