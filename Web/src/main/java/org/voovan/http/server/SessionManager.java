package org.voovan.http.server;

import org.voovan.http.message.packet.Cookie;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Method;
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

	private Method expirePutMethod = null;

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

		autoExpire();
	}

	/**
	 * 判断缓存session 的 map 是否支持自清除
	 * @return
	 */
	public boolean autoExpire(){

		if(expirePutMethod == null) {
			try {
				expirePutMethod = TReflect.findMethod(httpSessions.getClass(), "put", String.class, String.class, int.class);
			} catch (ReflectiveOperationException e) {
				expirePutMethod = null;
			}
		}

		return expirePutMethod == null ? false : true;
	}

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
	public void saveSession(HttpSession session) {
		if(!autoExpire()) {
			httpSessions.put(session.getId(), JSON.toJSON(session));
		}else{
			try {
				TReflect.invokeMethod(httpSessions, expirePutMethod, session.getId(), JSON.toJSON(session), session.getMaxInactiveInterval() / 1000);
			} catch (ReflectiveOperationException e) {
				httpSessions.put(session.getId(), JSON.toJSON(session));
			}
		}
	}

	/**
	 * 获取 Session
	 *
	 * @param id session Id
	 * @return HTTP-Session对象
	 */
	public HttpSession getSession(String id) {
		if (id!=null && httpSessions.containsKey(id)) {
			HttpSession httpSession = JSON.toObject(httpSessions.get(id), HttpSession.class);
			if(httpSession!=null) {
				httpSession.refresh();
				if (httpSession.isExpire()) {
					httpSession.removeFromSessionManager();
					return null;
				}
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
	public HttpSession getSession(Cookie cookie) {
		if (cookie!=null && httpSessions.containsKey(cookie.getValue())) {
			return getSession(cookie.getValue());
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
	public void removeSession(HttpSession seesion){
		removeSession(seesion.getId());
	}

	/**
	 * 获得 Session 如果没有对应的 session 则创建一个新的 Session
	 * @param request  HTTP 请求对象
	 * @return HTTP-Session对象
	 */
	public HttpSession newSession(HttpRequest request){

		HttpSession session = null;

		if(request.header().get("Host") != null) {
			session = new HttpSession(webConfig, this, request.getSocketSession());
		}else{
			Logger.warn("Create session cookie error, the request haven't an header of host.");
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
