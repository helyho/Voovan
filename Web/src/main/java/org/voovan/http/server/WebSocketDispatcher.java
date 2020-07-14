package org.voovan.http.server;

import org.voovan.http.message.HttpStatic;
import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.websocket.*;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.collection.Chain;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

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
	public static HashWheelTimer HEARTBEAT_WHEEL_TIMER = null;

	public static HashWheelTimer getHeartBeatWheelTimer() {
		if(HEARTBEAT_WHEEL_TIMER == null) {
			synchronized (IoSession.class) {
				if(HEARTBEAT_WHEEL_TIMER == null) {
					HEARTBEAT_WHEEL_TIMER = new HashWheelTimer("SocketIdle", 60, 1000);
					HEARTBEAT_WHEEL_TIMER.rotate();
				}
			}
		}

		return HEARTBEAT_WHEEL_TIMER;
	}

	private WebServerConfig webConfig;
	private SessionManager sessionManager;

	@NotSerialization
	private Map<IoSession, WebSocketSession> webSocketSessions;

	/**
	 * [Key] = Route path ,[Value] = WebSocketBizHandler对象
	 */
	private Map<String, RouterWrap<WebSocketRouter>>routers;

	public enum WebSocketEvent {
		OPEN, RECIVED, SENT, CLOSE, PING, PONG
	}

	/**
	 * 构造函数
	 * @param webConfig WEB 配置对象
	 * @param sessionManager session 管理器
	 */
	public WebSocketDispatcher(WebServerConfig webConfig, SessionManager sessionManager) {
		this.webConfig = webConfig;
		this.sessionManager = sessionManager;

		webSocketSessions = new ConcurrentHashMap<IoSession, WebSocketSession>();

		routers =  new TreeMap<String, RouterWrap<WebSocketRouter>>(new Comparator<String>() {
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
	public Map<String, RouterWrap<WebSocketRouter>> getRouters(){
		return routers;
	}

	/**
	 * 增加一个路由规则
	 *
	 * @param routePath 匹配路径
	 * @param handler WebSocketRouter 对象
	 */
	public void addRouteHandler(String routePath, WebSocketRouter handler) {
		routePath = HttpDispatcher.fixRoutePath(routePath);
		routers.put(routePath, new RouterWrap<WebSocketRouter>(HttpStatic.GET_STRING, routePath, handler));
	}

	/**
	 * 获取路由处理对象和注册路由
	 * @param request 请求对象
	 * @return 路由信息对象 [ 匹配到的已注册路由, WebSocketRouter对象 ]
	 */
	public RouterWrap<WebSocketRouter> findRouter(HttpRequest request){
		String requestPath = request.protocol().getPath();
		for (Map.Entry<String, RouterWrap<WebSocketRouter>> routeEntry : routers.entrySet()) {
			String routePath = routeEntry.getKey();
			if(HttpDispatcher.matchPath(requestPath, routePath, HttpDispatcher.routePath2RegexPath(routePath), webConfig.isMatchRouteIgnoreCase())) {
				//[ 匹配到的已注册路由, HttpRouter对象 ]
				return routeEntry.getValue();
			}
		}

		return null;
	}


	/**
	 * 过滤器解密函数,接收事件(onRecive)前调用
	 * 			onRecive事件前调用
	 * @param session  session 对象
	 * @param result   解码对象,上一个过滤器的返回值
	 * @return 解码后对象
	 * @throws WebSocketFilterException WebSocket过滤器异常
	 */
	public static Object filterDecoder(WebSocketSession session, Chain<WebSocketFilter> wsFilterChain, Object result) throws WebSocketFilterException {
		wsFilterChain.rewind();
		while (wsFilterChain.hasNext()) {
			WebSocketFilter fitler = wsFilterChain.next();
			result = fitler.decode(session, result);
			if (result == null) {
				break;
			}
		}
		return result;
	}

	/**
	 * 过滤器解密函数,接收事件(onRecive)前调用
	 * 			onRecive事件前调用
	 * @param session  session 对象
	 * @param result   解码对象,上一个过滤器的返回值
	 * @return 解码后对象
	 * @throws WebSocketFilterException WebSocket过滤器异常
	 */
	public static Object filterDecoder(WebSocketSession session, Object result) throws WebSocketFilterException {
		return filterDecoder(session, (Chain<WebSocketFilter>) session.getWebSocketRouter().getWebSocketFilterChain().clone(), result);
	}

	/**
	 * 使用过滤器编码结果
	 * @param session      Session 对象
	 * @param wsFilterChain 过滤器链
	 * @param result	   需编码的对象
	 * @return  编码后的对象
	 * @throws WebSocketFilterException WebSocket过滤器异常
	 */
	public static Object filterEncoder(WebSocketSession session, Chain<WebSocketFilter> wsFilterChain, Object result) throws WebSocketFilterException {
		wsFilterChain.rewind();
		while (wsFilterChain.hasPrevious()) {
			WebSocketFilter fitler = wsFilterChain.previous();
			result = fitler.encode(session, result);
			if (result == null) {
				break;
			}
		}

		if(result == null){
			return null;
		} else if(result instanceof ByteBuffer) {
			return (ByteBuffer)result;
		}else{
			throw new WebSocketFilterException("Send object must be ByteBuffer, " +
					"please check you filter be sure the latest filter return Object's type is ByteBuffer.");
		}
	}

	/**
	 * 过滤器解密函数,接收事件(onRecive)前调用
	 * 			onRecive事件前调用
	 * @param session  session 对象
	 * @param result   解码对象,上一个过滤器的返回值
	 * @return 解码后对象
	 * @throws WebSocketFilterException WebSocket过滤器异常
	 */
	public static Object filterEncoder(WebSocketSession session, Object result) throws WebSocketFilterException {
		return filterEncoder(session, (Chain<WebSocketFilter>) session.getWebSocketRouter().getWebSocketFilterChain().clone(), result);
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

		//[ 匹配到的已注册路由, WebSocketRouter对象 ]
		RouterWrap<WebSocketRouter> routerWrap = findRouter(request);

		if (routerWrap != null) {
//			String routePath = (String)routerInfo.getThread(0);
			WebSocketRouter webSocketRouter = routerWrap.getRouter();

			WebSocketSession webSocketSession = disposeSession(request, webSocketRouter);

			Object[] attachment = (Object[])session.getAttachment();
			Chain<WebSocketFilter> webFilterChain = (Chain<WebSocketFilter>) attachment[2];
			if(webFilterChain == null) {
				webFilterChain = (Chain<WebSocketFilter>) webSocketRouter.getWebSocketFilterChain().clone();
				attachment[2] =  webFilterChain;
			}

			// 获取路径变量
			ByteBuffer responseMessage = null;

			try {
				Object result = byteBuffer;
				//WebSocket 事件处理
				if (event == WebSocketEvent.OPEN) {
					result = webSocketRouter.onOpen(webSocketSession);
					//封包
					responseMessage = (ByteBuffer) filterEncoder(webSocketSession, webFilterChain, result);
				} else if (event == WebSocketEvent.RECIVED) {
					//解包
					result = filterDecoder(webSocketSession, webFilterChain, result);
					//触发 onRecive 事件
					if(result!=null) {
						result = webSocketRouter.onRecived(webSocketSession, result);
					}
					//封包
					responseMessage = (ByteBuffer) filterEncoder(webSocketSession, webFilterChain, result);
				}

				//将返回消息包装称WebSocketFrame
				if (responseMessage != null) {
					return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, false, responseMessage);
				}

				if (event == WebSocketEvent.SENT) {
					//封包
					result = filterDecoder(webSocketSession, webFilterChain, byteBuffer);
					webSocketRouter.onSent(webSocketSession, result);
				} else if (event == WebSocketEvent.CLOSE) {
					webSocketRouter.onClose(webSocketSession);

					//清理 webSocketSessions 中的 WebSocketSession
					webSocketSessions.remove(session);
				} else if (event == WebSocketEvent.PING) {
					return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PONG, false, byteBuffer);
				} else if (event == WebSocketEvent.PONG) {
					final IoSession poneSession = session;
					if(poneSession.isConnected()) {
						WebSocketDispatcher.getHeartBeatWheelTimer().addTask(new HashWheelTask() {
							@Override
							public void run() {
								try {
									if(poneSession.socketContext().isConnected()) {
										poneSession.syncSend(WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, false, null));
									}
								} catch (SendMessageException e) {
									poneSession.close();
									Logger.error("Send WebSocket ping error", e);
								} finally {
									this.cancel();
								}
							}
						}, poneSession.socketContext().getReadTimeout() / 1000/ 3);
					}
				}
			} catch (WebSocketFilterException e) {
				Logger.error(e);
			}
		}
		// 没有找寻到匹配的路由处理器
		else {
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
		request.setSessionManager(sessionManager);
		HttpSession httpSession = request.getSession();
		IoSession socketSession = request.getSocketSession();

		//如果 session 不存在,创建新的 session
		if (!webSocketSessions.containsKey(socketSession)) {
			// 构建 session
			WebSocketSession webSocketSession = new WebSocketSession(httpSession.getSocketSession(), webSocketRouter, WebSocketType.SERVER);

			webSocketSessions.put(socketSession, webSocketSession);
			return webSocketSession;
		} else {
			return webSocketSessions.get(socketSession);
		}

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
		HttpSessionState httpSessionState = WebServerHandler.getAttachment(session);
		//检查是否是WebSocket
		if (httpSessionState.isWebSocket()) {
			// 触发一个 WebSocket Close 事件
			process(WebSocketEvent.CLOSE, session, httpSessionState.getHttpRequest(), null);
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
