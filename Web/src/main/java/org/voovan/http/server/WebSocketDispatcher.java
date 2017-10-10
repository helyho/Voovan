package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

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
		OPEN, RECIVED, SENT, CLOSE, PING, PONG
	}

	/**
	 * 构造函数
	 * @param webConfig WEB 配置对象
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
				} else if(o1.equals(o2)){
					return 0;
				} else{
					return 1;
				}
			}
		});
	}

	/**
	 * 获取 WebSocket 的路由配置
	 * @return 路由配置信息
	 */
	public Map<String, WebSocketRouter> getRoutes(){
		return routes;
	}

	/**
	 * 增加一个路由规则
	 *
	 * @param routeRegexPath 匹配路径
	 * @param handler WebSocketRouter 对象
	 */
	public void addRouteHandler(String routeRegexPath, WebSocketRouter handler) {
		routeRegexPath = HttpDispatcher.fixRoutePath(routeRegexPath);
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

				// 获取路径变量
				ByteBuffer responseMessage = null;

				try {
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
					}

					//将返回消息包装称WebSocketFrame
					if (responseMessage != null) {
						return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, false, responseMessage);
					}

					if (event == WebSocketEvent.SENT) {
						//封包
						result = webSocketRouter.filterDecoder(webSocketSession, byteBuffer);
						webSocketRouter.onSent(webSocketSession, result);
					} else if (event == WebSocketEvent.CLOSE) {
						webSocketRouter.onClose(webSocketSession);
					} else if (event == WebSocketEvent.PING) {
						return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PONG, false, byteBuffer);
					} else if (event == WebSocketEvent.PONG) {
						final IoSession poneSession = session;
						Global.getThreadPool().execute(new Runnable() {
							@Override
							public void run() {
								TEnv.sleep(poneSession.socketContext().getReadTimeout() / 3);
								try {
									poneSession.syncSend(WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, false, null));
								} catch (SendMessageException e) {
									poneSession.close();
									Logger.error("Send WebSocket ping error", e);
								}
							}
						});
					}
				}catch(WebSocketFilterException e){
					Logger.error(e);
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
			WebSocketSession webSocketSession =
					new WebSocketSession(httpSession.getSocketSession(), webSocketRouter,
							request.getRemoteAddres(), request.getRemotePort());

			httpSession.setWebSocketSession(webSocketSession);
		}

		httpSession.getWebSocketSession().setSocketSession(request.getSocketSession());


		return httpSession.getWebSocketSession();

	}

	/**
	 * 触发 WebSocket Open 事件
	 * @param session socket 会话对象
	 * @param request http 请求对象
	 * @return WebSocketFrame WebSocket 帧
	 */
	public WebSocketFrame fireOpenEvent(IoSession session, HttpRequest request){
		//触发 onOpen 事件
		return process(WebSocketEvent.OPEN, session, request, null);
	}

	/**
	 * 触发 WebSocket Received 事件
	 * @param session socket 会话对象
	 * @param request http 请求对象
	 * @param byteBuffer ping的报文数据
	 * @return WebSocketFrame WebSocket 帧
	 */
	public  WebSocketFrame fireReceivedEvent(IoSession session, HttpRequest request, ByteBuffer byteBuffer){
		return process(WebSocketEvent.RECIVED, session, request, byteBuffer);
	}

	/**
	 * 触发 WebSocket Sent 事件
	 * @param session socket 会话对象
	 * @param request http 请求对象
	 * @param byteBuffer ByteBuffer 对象
	 */
	public void fireSentEvent(IoSession session, HttpRequest request, ByteBuffer byteBuffer){
		process(WebSocketEvent.SENT, session, request, byteBuffer);
	}

	/**
	 * 出发 Close 事件
	 * @param session HTTP-Session 对象
	 */
	public void fireCloseEvent(IoSession session){
		//检查是否是WebSocket
		if ("WebSocket".equals(WebServerHandler.getAttribute(session, WebServerHandler.SessionParam.TYPE))) {
			// 触发一个 WebSocket Close 事件
			process(WebSocketEvent.CLOSE, session, (HttpRequest) WebServerHandler.getAttribute(session, WebServerHandler.SessionParam.HTTP_REQUEST), null);
		}
	}

	/**
	 * 触发 WebSocket Ping 事件
	 * @param session socket 会话对象
	 * @param request http 请求对象
	 * @param byteBuffer ping的报文数据
	 * @return WebSocketFrame WebSocket 帧
	 */
	public WebSocketFrame firePingEvent(IoSession session, HttpRequest request, ByteBuffer byteBuffer){
		return process(WebSocketEvent.PING, session, request, byteBuffer);
	}

	/**
	 * 触发 WebSocket Pone 事件
	 * @param session socket 会话对象
	 * @param request http 请求对象
	 * @param byteBuffer ByteBuffer 对象
	 */
	public void firePoneEvent(IoSession session, HttpRequest request, ByteBuffer byteBuffer){
		process(WebSocketEvent.PONG, session, request, byteBuffer);
	}
}
