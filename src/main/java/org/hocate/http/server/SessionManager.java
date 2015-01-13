package org.hocate.http.server;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class SessionManager {
	private static Map<String, HttpSession>	sessions	= new Hashtable<String, HttpSession>();

	/**
	 * 增加 Session
	 * 
	 * @param session
	 */
	public static synchronized void addSession(HttpSession session) {
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
	public static synchronized HttpSession getSession(String id) {
		clearInvalidSession();
		if (sessions.containsKey(id)) {
			return sessions.get(id);
		}
		return null;
	}

	/**
	 * 获取失效的 session
	 */
	public static synchronized List<HttpSession> getInvalidSession() {
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
	public static synchronized void clearInvalidSession() {
		List<HttpSession> needRemove = getInvalidSession();
		for(HttpSession session : needRemove){
			sessions.remove(session.getId());
		}
	}

}
