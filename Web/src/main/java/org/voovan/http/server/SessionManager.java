package org.voovan.http.server;

import org.voovan.http.message.packet.Cookie;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.tools.collection.CacheMap;
import org.voovan.tools.collection.CachedHashMap;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

/**
 * WebServer session 管理器
 *
 * 用到的 CacheMap 接口的实现类的方法
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SessionManager{
	private CacheMap<String, Object> httpSessions;

	private WebServerConfig webConfig;


	/**
	 * 构造函数
	 * @param webConfig Web 服务配置对象
	 */
	public SessionManager(WebServerConfig webConfig){
		this.webConfig = webConfig;
		httpSessions = getSessionContainer();

		if(httpSessions instanceof CachedHashMap){
			((CachedHashMap)httpSessions).create();
		}

		if(httpSessions == null){
			httpSessions = new CachedHashMap<String, Object>();
			Logger.warn("Create session container from config file failed,now use CachedHashMap as defaul session container.");
		}
	}

	/**
	 * 获取 Session 容器
	 *
	 * @return Session 容器 Map
	 */
	public CacheMap<String, Object> getSessionContainer(){
		if(httpSessions!=null){
			return httpSessions;
		}else{
			try {
				String sessionContainerClassName = webConfig.getSessionContainer();
				//根据 Class 构造一个 Session 容器
				return TReflect.newInstance(sessionContainerClassName);
			} catch (ReflectiveOperationException e) {
				Logger.error("SessionManager.getSessionContainer Reflective operation error",e);
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
		if(httpSessions instanceof CachedHashMap){
			httpSessions.put(session.getId(), session, session.getMaxInactiveInterval());
		} else {
			httpSessions.put(session.getId(), JSON.toJSON(session), session.getMaxInactiveInterval());
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

			Object sessionObject  = httpSessions.getAndRefresh(id);

			HttpSession httpSession = null;

			if(sessionObject instanceof HttpSession){
				httpSession = (HttpSession)sessionObject;
			}

			if(sessionObject instanceof String) {
				httpSession = JSON.toObject((String)sessionObject, HttpSession.class);
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
		if(httpSessions.containsKey(id)) {
			httpSessions.remove(id);
		}
	}

	/**
	 * 移除会话
	 * @param session HttpSession 对象
	 */
	public void removeSession(HttpSession session){
		if(session!=null && session.getId()!=null) {
			removeSession(session.getId());
		}
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
