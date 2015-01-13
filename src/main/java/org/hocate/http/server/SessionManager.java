package org.hocate.http.server;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
	private static Map<String,HttpSession> sessions = new HashMap<String,HttpSession>();
	
	/**
	 * 增加 Session
	 * @param session
	 */
	public static void addSession(HttpSession session){
		clearInvalidSession();
		if(!sessions.containsKey(session.getId())){
			sessions.put(session.getId(), session);
		}
	}
	
	/**
	 * 获取 Session
	 * @param id
	 * @return
	 */
	public static HttpSession getSession(String id){
		clearInvalidSession();
		if(sessions.containsKey(id)){
			return sessions.get(id);
		}
		return null;
	}
	
	/**
	 * 清理失效的 session
	 */
	public static void clearInvalidSession(){
		for(HttpSession session : sessions.values()){
			if(session.isInvalid()){
				sessions.remove(session.getId());
			}
		}
	}

}	
