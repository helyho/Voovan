package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebServer session 管理器
 * 在 nosql 中指定默认的超时 Session 清理节点(key=SESSION_MANAGER_SERVER)的服务名
 * 可以保证集群中只有一个节点进行超时 Session 的清理工作
 *
 * 用到的 Map 接口的实现类的方法: keySet, containsKey, put, get, remove
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SessionManager{
	private  Map<String, String> httpSessions;

	private WebServerConfig webConfig;
	/**
	 * 构造函数
	 * @param webConfig Web 服务配置对象
	 */
	public SessionManager(WebServerConfig webConfig){
		this.webConfig = webConfig;
		httpSessions = getSessionContainer();
		if(httpSessions == null){
			httpSessions = new ConcurrentHashMap<String, String>();
			Logger.warn("Create session container from config file failed,now use defaul session container.");
		}

		String serverName = httpSessions.get("SESSION_MANAGER_SERVER");

		if(serverName==null){
			serverName = webConfig.getServerName();
			httpSessions.put("SESSION_MANAGER_SERVER", serverName);
		}

//		initKeepAliveTimer();

	}

//	/**
//	 * 初始化连接保持 Timer
//	 */
//	public void initKeepAliveTimer(){
//
//		Global.getHashWheelTimer().addTask(new HashWheelTask() {
//			@Override
//			public void run() {
//
//				String serverName = httpSessions.get("SESSION_MANAGER_SERVER");
//
//				if(serverName==null){
//					serverName = webConfig.getServerName();
//					httpSessions.put("SESSION_MANAGER_SERVER", serverName);
//				}
//
//				//判断是否是 Server 管理节点, 保证只有一个节点处理 Session 移除动作
//				if(serverName.equals(webConfig.getServerName())) {
//					//遍历所有的 session
//					for (String key : httpSessions.keySet().toArray(new String[]{})) {
//						HttpSession session = JSON.toObject(httpSessions.get(key), HttpSession.class);
//						if (session != null && session.isInvalid()) {
//							removeSession(session);
//						}
//					}
//				}
//			}
//		}, 60, true);
//	}

	/**
	 * 获取 Session 容器
	 *
	 * @return Session 容器 Map
	 */
	public Map<String, String> getSessionContainer(){
		if(httpSessions!=null){
			return httpSessions;
		}else{
			try {
				String sessionContainerClassName = webConfig.getSessionContainer();
				//根据 Class 构造一个 Session 容器
				return TReflect.newInstance(sessionContainerClassName);
			} catch (ReflectiveOperationException e) {
				Logger.error("Reflective operation error",e);
				return null;
			}
		}
	}

	/**
	 * 保存 Session
	 *
	 * @param session HTTP-Session对象
	 */
	public void saveSession(org.voovan.http.server.HttpSession session) {
		httpSessions.put(session.getId(), JSON.toJSON(session));
	}

	/**
	 * 获取 Session
	 *
	 * @param id session Id
	 * @return HTTP-Session对象
	 */
	public org.voovan.http.server.HttpSession getSession(String id) {
		if (id!=null && httpSessions.containsKey(id)) {
			org.voovan.http.server.HttpSession httpSession = JSON.toObject(httpSessions.get(id), org.voovan.http.server.HttpSession.class);
			httpSession.refresh();
			if(httpSession.isInvalid()){
				httpSession.removeFromSessionManager();
				return null;
			}
			return httpSession;
		}
		return null;
	}

	/**
	 * 获取 Session
	 *
	 * @param cookie cookie 对象
	 * @return HTTP-Session对象
	 */
	public org.voovan.http.server.HttpSession getSession(Cookie cookie) {
		if (cookie!=null && httpSessions.containsKey(cookie.getValue())) {
			org.voovan.http.server.HttpSession httpSession = getSession(cookie.getValue());
			httpSession.refresh();
			return httpSession;
		}
		return null;
	}

	/**
	 * 判断 Session 是否存在
	 * @param cookie cookie 对象
	 * @return 是否存在
	 */
	public boolean containsSession(Cookie cookie) {
		if(cookie==null){
			return false;
		} else {
			return getSession(cookie) != null;
		}
	}

	/**
	 * 移除会话
	 * @param id 会话 id
	 */
	public void removeSession(String id){
		httpSessions.remove(id);
	}


	/**
	 * 移除会话
	 * @param seesion HttpSession 对象
	 */
	public void removeSession(org.voovan.http.server.HttpSession seesion){
		removeSession(seesion.getId());
	}

	/**
	 * 获得一个新的 Session
	 * @param request  HTTP 请求对象
	 * @param response  HTTP 响应对象
	 * @return HTTP-Session对象
	 */
	public HttpSession newHttpSession(HttpRequest request, HttpResponse response){


		HttpSession session = null;

		//获取请求的 Cookie中的session标识
		Cookie sessionCookie = request.getCookie(WebContext.getSessionName());
		if(sessionCookie!=null) {
			session = getSession(sessionCookie.getValue());
		}

		if(session==null) {
			if(request.header().get("Host") != null) {
				session = new HttpSession(webConfig, this, request.getSocketSession());
				this.saveSession(session);

				//创建 Cookie
				Cookie cookie = Cookie.newInstance(request, WebContext.getSessionName(),
						session.getId(), webConfig.getSessionTimeout() * 60);

				//响应增加Session 对应的 Cookie
				response.cookies().add(cookie);
			}else{
				Logger.warn("Create session cookie error, the request haven't an header of host.");
			}
		} else{
			session.init(this, request.getSocketSession());
		}

		return session;
	}

	/**
	 * 构造一个 SessionManager
	 * @param config WEB 配置对象
	 * @return SessionManager对象
	 */
	public static SessionManager newInstance(WebServerConfig config){
		return new SessionManager(config);
	}
}
