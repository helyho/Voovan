package org.voovan.http.server;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.voovan.tools.TObject;
import org.voovan.tools.TReflect;
import org.voovan.tools.log.Logger;

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
	private WebServerConfig config;
	
	/**
	 * 构造函数
	 * @param config
	 */
	public SessionManager(WebServerConfig config){
		this.config = config;
		sessions = getSessionContainer();
		if(sessions == null){
			sessions = new Hashtable<String, HttpSession>();
			Logger.warn("Create session container from config file failed,now use defaul session container.");
		}
	}

	/**
	 * 获取 Session 容器
	 */
	public Map<String, HttpSession> getSessionContainer(){
		try {
			String className = config.getSessionContainer();
			Class<?> sessionContainerClass = Class.forName(className);
			Map<String, HttpSession> sessionContainer = TObject.cast(TReflect.newInstance(sessionContainerClass));
			return sessionContainer;
		} catch (Exception e) {
			Logger.error("Class SessionManager Error: "+e.getMessage());
			return null;
		}
	}
	
	/**
	 * 增加 Session
	 * 
	 * @param session
	 */
	public synchronized void addSession(HttpSession session) {
		if (!sessions.containsKey(session.getId())) {
			sessions.put(session.getId(), session);
		}
	}

	/**
	 * 获取 Session
	 * 
	 * @param id
	 * @return
	 */
	public synchronized HttpSession getSession(String id) {
		clearInvalidSession();
		if (sessions.containsKey(id)) {
			return sessions.get(id);
		}
		return null;
	}

	/**
	 * 获取失效的 session
	 */
	public synchronized List<HttpSession> getInvalidSession() {
		List<HttpSession> needRemove = new ArrayList<HttpSession>();
		for (HttpSession session : sessions.values()) {
			if (session.isInvalid()) {
				needRemove.add(session);
			}
		}
		return needRemove;
	}
	
	/**
	 * 获取失效的 session
	 */
	public synchronized void clearInvalidSession() {
		List<HttpSession> needRemove = getInvalidSession();
		for(HttpSession session : needRemove){
			sessions.remove(session.getId());
		}
	}
	
	/**
	 * 获得一个新的 Session
	 * @return
	 */
	public HttpSession newHttpSession(){
		return new HttpSession(config);
	}
	
	public static SessionManager newInstance(WebServerConfig config){
		return new SessionManager(config);
	}
}
