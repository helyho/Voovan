package org.hocate.http.server;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.hocate.log.Logger;

public class SessionManager{
	private  Map<String, HttpSession>	sessions;
	
	public SessionManager(Map<String, HttpSession> sessionMap){
		if(sessionMap == null){
			sessions = new Hashtable<String, HttpSession>();
			Logger.warn("Create session container from config file failed,now use defaul session container.");
		}else{
			sessions = sessionMap;
		}
	}

	/**
	 * 增加 Session
	 * 
	 * @param session
	 */
	public synchronized void addSession(HttpSession session) {
		clearInvalidSession();
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
		List<HttpSession> needRemove = new Vector<HttpSession>();
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
		return new HttpSession();
	}
}
