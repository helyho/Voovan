package org.voovan.http.server;

import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketFrame.Opcode;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
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
	private SessionManager sessionManager;

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
	public WebSocketDispatcher(WebServerConfig webConfig, SessionManager sessionManager) {
		this.webConfig = webConfig;
		this.sessionManager = sessionManager;

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
	 * @param byteBuffer 对象, 保存 WebSocket 数据
	 * @return WebSocket 帧对象
	 */
	public WebSocketFrame process(WebSocketEvent event, IoSession session, HttpRequest request, ByteBuffer byteBuffer) {

		String requestPath = request.protocol().getPath();

		boolean isMatched = false;
		for (Map.Entry<String,WebSocketRouter> routeEntry : routes.entrySet()) {
			String routePath = routeEntry.getKey();
			// 路由匹配
			isMatched = HttpDispatcher.matchPath(requestPath, routePath, webConfig.isMatchRouteIgnoreCase());
			if (isMatched) {
				// 获取路由处理对象
				WebSocketRouter webSocketRouter = routeEntry.getValue();

				WebSocketSession webSocketSession = disposeSession(request, webSocketRouter);

				webSocketRouter.setSession(webSocketSession);

				// 获取路径变量
				ByteBuffer responseMessage = null;

				Object result = byteBuffer;
				//WebSocket 事件处理
				if (event == WebSocketEvent.OPEN) {
					result = webSocketRouter.onOpen(webSocketSession);
					//封包
					responseMessage = (ByteBuffer) webSocketRouter.filterEncoder(webSocketSession, result);
				} else if (event == WebSocketEvent.RECIVED) {
					//解包
					result = webSocketRouter.filterDecoder(webSocketSession, result);
					//触发 onRecive 事件
					result = webSocketRouter.onRecived(webSocketSession, result);
					//封包
					responseMessage = (ByteBuffer) webSocketRouter.filterEncoder(webSocketSession, result);
				} else if (event == WebSocketEvent.SENT) {
					//封包
					result = webSocketRouter.filterDecoder(webSocketSession, byteBuffer);
					webSocketRouter.onSent(webSocketSession, result);
				} else if (event == WebSocketEvent.CLOSE) {
					webSocketRouter.onClose(webSocketSession);
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
	 * 处理 WebSocketSession
	 * @param request Http 请求对象
	 * @param webSocketRouter websocket 路由处理
	 * @return WebSocketSession对象
	 */
	public WebSocketSession disposeSession(HttpRequest request, WebSocketRouter webSocketRouter){

		HttpSession httpSession = request.getSession();
		//如果 session 不存在,创建新的 session
		if (httpSession.getWebSocketSession()==null) {
			// 构建 session
			WebSocketSession webSocketSession = new WebSocketSession(httpSession.getSocketSession(), webSocketRouter);
			httpSession.setWebSocketSession(webSocketSession);
			// 请求增加 Session
			request.setSession(httpSession);
		}

		return httpSession.getWebSocketSession();

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
