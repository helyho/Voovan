package org.voovan.http.server;

import org.voovan.http.message.packet.Cookie;
import org.voovan.tools.TReflect;
import org.voovan.tools.log.Logger;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * HTTPServer session 管理器
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SessionManager{
	private  Map<String, HttpSession>	sessions;
	private WebServerConfig webConfig;
	
	/**
	 * 构造函数
	 * @param webConfig Web 服务配置对象
	 */
	public SessionManager(WebServerConfig webConfig){
		this.webConfig = webConfig;
		sessions = getSessionContainer();
		if(sessions == null){
			sessions = new Hashtable<String, HttpSession>();
			Logger.warn("Create session container from config file failed,now use defaul session container.");
		}
	}

	/**
	 * 获取 Session 容器
	 *
	 * @return Session 容器 Map
	 */
	public Map<String, HttpSession> getSessionContainer(){
		if(sessions!=null){
			return sessions;
		}else{
			try {
				String sessionContainerClassName = webConfig.getSessionContainer();
				//根据 Class 构造一个 Session 容器
				return TReflect.newInstance(sessionContainerClassName);
			} catch (ReflectiveOperationException e) {
				Logger.error("Reflective operation error.",e);
				return null;
			}
		}
	}
	
	/**
	 * 增加 Session
	 * 
	 * @param session HTTP-Session对象
	 */
	public void addSession(HttpSession session) {
		synchronized(sessions){
			if (!sessions.containsKey(session.getId())) {
				sessions.put(session.getId(), session);
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
		synchronized(sessions){
			clearInvalidSession();
			if (id!=null && sessions.containsKey(id)) {
				return sessions.get(id).refresh();
			}
			return null;
		}
	}
	
	/**
	 * 获取 Session
	 * 
	 * @param cookie cookie 对象
	 * @return HTTP-Session对象
	 */
	public HttpSession getSession(Cookie cookie) {
		synchronized(sessions){
			clearInvalidSession();
			if (cookie!=null && sessions.containsKey(cookie.getValue())) {
				return sessions.get(cookie.getValue()).refresh();
			}
			return null;
		}
	}

	/**
	 * 判断 Session 是否存在
	 * @param cookie cookie 对象
	 * @return 是否存在
	 */
	public boolean containsSession(Cookie cookie) {
		synchronized(sessions){
			if(cookie==null){
				return false;
			} else { 
				return getSession(cookie) != null;
			}
		}
	}
	
	/**
	 * 获取失效的 session
	 *
	 * @return session 失效时间
	 */
	public List<HttpSession> getInvalidSession() {
		synchronized(sessions){
			List<HttpSession> needRemove = new ArrayList<HttpSession>();
			for (HttpSession session : sessions.values()) {
				if (session.isInvalid()) {
					needRemove.add(session);
				}
			}
			return needRemove;
		}
	}
	
	/**
	 * 清理失效的 session
	 */
	public void clearInvalidSession() {
		synchronized(sessions){
			List<HttpSession> needRemove = getInvalidSession();
			for(HttpSession session : needRemove){
				sessions.remove(session.getId());
			}
		}
	}
	
	/**
	 * 获得一个新的 Session
	 * @param request  HTTP 请求对象
	 * @param response  HTTP 响应对象
	 * @return HTTP-Session对象
	 */
	public HttpSession newHttpSession(HttpRequest request,HttpResponse response){
		HttpSession session  = new HttpSession(webConfig);
		
		this.addSession(session);
		
		//创建 Cookie
		Cookie cookie = Cookie.newInstance(request, WebContext.getSessionName(), 
				session.getId(),webConfig.getSessionTimeout()*60);
		
		//响应增加Session 对应的 Cookie
		response.cookies().add(cookie);
		
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
