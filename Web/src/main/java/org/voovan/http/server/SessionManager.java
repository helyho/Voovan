package org.voovan.http.server;

import org.voovan.http.message.packet.Cookie;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebServer session 管理器
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SessionManager{
	private  Map<String, HttpSession>	httpSessions;
	private WebServerConfig webConfig;
	private Timer checkSessionTimer;
	/**
	 * 构造函数
	 * @param webConfig Web 服务配置对象
	 */
	public SessionManager(WebServerConfig webConfig){
		this.webConfig = webConfig;
		httpSessions = getSessionContainer();
		if(httpSessions == null){
			httpSessions = new ConcurrentHashMap<String, HttpSession>();
			Logger.warn("Create session container from config file failed,now use defaul session container.");
		}

		checkSessionTimer = new Timer("VOOVAN_WEB@CHECK_SESSION_TASK");
		initKeepAliveTimer();
	}

	/**
	 * 初始化连接保持 Timer
	 */
	public void initKeepAliveTimer(){

		TimerTask checkSessionTask = new TimerTask() {
			@Override
			public void run() {

				//遍历所有的 session
				for(String sessionId : httpSessions.keySet().toArray(new String[]{})){

					HttpSession session = httpSessions.get(sessionId);

					if(session.isInvalid()){
						session.removeFromSessionManager();
					}
				}
			}
		};
		checkSessionTimer.schedule(checkSessionTask, 1 , 60*1000);
	}

	/**
	 * 获取 Session 容器
	 *
	 * @return Session 容器 Map
	 */
	public Map<String, HttpSession> getSessionContainer(){
		if(httpSessions!=null){
			return httpSessions;
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
        if (!httpSessions.containsKey(session.getId())) {
            httpSessions.put(session.getId(), session);
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
			HttpSession httpSession = httpSessions.get(id).refresh();
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
	public HttpSession getSession(Cookie cookie) {
        if (cookie!=null && httpSessions.containsKey(cookie.getValue())) {
			HttpSession httpSession = httpSessions.get(cookie.getValue()).refresh();
			if(httpSession.isInvalid()){
				httpSession.removeFromSessionManager();
				return null;
			}
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

	public void removeSession(HttpSession seesion){
		httpSessions.remove(seesion.getId());
	}
	
	/**
	 * 获得一个新的 Session
	 * @param request  HTTP 请求对象
	 * @param response  HTTP 响应对象
	 * @return HTTP-Session对象
	 */
	public HttpSession newHttpSession(HttpRequest request,HttpResponse response){
		HttpSession session  = new HttpSession(webConfig, this);
		
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
