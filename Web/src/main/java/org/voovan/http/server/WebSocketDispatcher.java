package org.voovan.http.server;

import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketFrame.Opcode;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.network.IoSession;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

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
	private WebServerConfig webConfig;

	/**
	 * [Key] = Route path ,[Value] = WebSocketBizHandler对象
	 */
	private Map<String, WebSocketRouter> routes;

	public enum WebSocketEvent {
		OPEN, RECIVED, SENT, CLOSE
	}

	/**
	 * 构造函数
	 * 
	 * @param webConfig WEB 配置对象
	 *            根目录
	 */
	public WebSocketDispatcher(WebServerConfig webConfig) {
		this.webConfig = webConfig;
		routes =  new TreeMap<String, WebSocketRouter>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if(o1.length() > o2.length()){
					return -1;
				} else if(o1.length() < o2.length()){
					return 1;
				} else {
					return 0;
				}
			}
		});
	}

	/**
	 * 增加一个路由规则
	 * 
	 * @param routeRegexPath 匹配路径
	 * @param handler WebSocketRouter 对象
	 */
	public void addRouteHandler(String routeRegexPath, WebSocketRouter handler) {
		routes.put(routeRegexPath, handler);
	}

	/**
	 * 路由处理函数
	 * 
	 * @param event     WebSocket 事件
	 * @param session   socket连接会话
	 * @param request   HTTP 请求对象
	 * @param bytebuffer bytebuffer 对象, 保存 WebSocket 数据
	 * @return WebSocket 帧对象
	 */
	public WebSocketFrame process(WebSocketEvent event, IoSession session, HttpRequest request, ByteBuffer bytebuffer) {

		String requestPath = request.protocol().getPath();

		boolean isMatched = false;
		for (Map.Entry<String,WebSocketRouter> routeEntry : routes.entrySet()) {
			String routePath = routeEntry.getKey();
			// 路由匹配
			isMatched = HttpDispatcher.matchPath(requestPath, routePath, webConfig.isMatchRouteIgnoreCase());
			if (isMatched) {
				// 获取路由处理对象
				WebSocketRouter webSocketRouter = routeEntry.getValue();
				webSocketRouter.setSession(session);

				// 获取路径变量
				ByteBuffer responseMessage = null;

				//WebSocket 事件处理
				if (event == WebSocketEvent.OPEN) {
					responseMessage = webSocketRouter.onOpen();
				} else if (event == WebSocketEvent.RECIVED) {
					responseMessage = webSocketRouter.onRecived(bytebuffer);
				} else if (event == WebSocketEvent.SENT) {
					webSocketRouter.onSent(bytebuffer);
				} else if (event == WebSocketEvent.CLOSE) {
					webSocketRouter.onClose();
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
	 * @param session HTTP-Session 对象
	 */
	public void fireCloseEvent(IoSession session){
		//检查是否是WebSocket
		if ("WebSocket".equals(WebServerHandler.getAttribute(session, WebServerHandler.SessionParam.TYPE))) {
				// 触发一个 WebSocket Close 事件
				process(WebSocketEvent.CLOSE, session, WebServerHandler.getAttribute(session, WebServerHandler.SessionParam.HTTP_REQUEST), null);
			}
	}
}
