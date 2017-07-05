package org.voovan.http.server;

import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.network.IoSession;
import org.voovan.tools.TString;
import org.voovan.tools.UniqueId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebServer session 类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpSession {
	private Map<String,Object> attributes;
	private String id ;
	private int maxInactiveInterval;
	private long lastTimeillis;
	private SessionManager sessionManager;
	private IoSession socketSession;
	private WebSocketSession webSocketSession;

	
	/**
	 * 构造函数
	 *
	 * @param config  WEB服务配置对象
	 * @param sessionManager Session管理器
	 */
	public HttpSession(WebServerConfig config, SessionManager sessionManager, IoSession socketSession){
		attributes = new ConcurrentHashMap<String, Object>();
		//生成一个随机的 ID 用作唯一标识
		this.id = TString.generateShortUUID();
		lastTimeillis = System.currentTimeMillis();
		int sessionTimeout = config.getSessionTimeout();
		this.maxInactiveInterval = sessionTimeout*60*1000;
		this.sessionManager = sessionManager;
		this.socketSession = socketSession;
	}

	/**
	 * 获取 WebSocket 会话对象
	 * @return socket 会话对象
	 */
	protected WebSocketSession getWebSocketSession() {
		return webSocketSession;
	}

	/**
	 * 设置 socket 会话对象
	 * @param webSocketSession WebSocket 会话对象
	 */
	protected void setWebSocketSession(WebSocketSession webSocketSession) {
		this.webSocketSession = webSocketSession;
	}

	/**
	 * 获取 socket 会话对象
	 * @return socket 会话对象
	 */
	public IoSession getSocketSession() {
		return socketSession;
	}

	/**
	 * 设置 socket 会话对象
	 * @param socketSession socket 会话对象
	 */
	public void setSocketSession(IoSession socketSession) {
		this.socketSession = socketSession;
	}

	/**
	 * 刷新 Session 的超时时间
	 *
	 * @return HTTP-Session 对象
	 */
	public HttpSession refresh(){
		lastTimeillis = System.currentTimeMillis();
		return this;
	}
	
	/**
	 * 获取当前 Session 属性
	 * @param name 属性名
	 * @return 属性值
	 */
	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	/**
	 * 判断当前 Session 属性是否存在
	 * @param name 属性名
	 * @return true: 存在, false: 不存在
	 */
	public boolean containAttribute(String name) {
		return attributes.containsKey(name);
	}

	/**
	 * 设置当前 Session 属性
	 * @param name	属性名
	 * @param value	属性值
	 */
	public void setAttribute(String name,Object value) {
		attributes.put(name, value);
	}
	
	/**
	 *  删除当前 Session 属性
	 * @param name	属性名
	 */
	public void removeAttribute(String name) {
		attributes.remove(name);
	}

	/**
	 * 获取 Session 管理器
	 * @return Session 管理器
	 */
	protected SessionManager getSessionManager() {
		return sessionManager;
	}

	/**
	 * 设置Session 管理器
	 * @param sessionManager Session 管理器
	 */
	protected void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public void removeFromSessionManager(){
		sessionManager.removeSession(this);
	}

	/**
	 * 获取 Session ID
	 *
	 * @return   Session ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * 获取最大活动时间
	 *
	 * @return 最大活动时间
	 */
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	/**
	 * 设置最大活动时间
	 *
	 * @param maxInactiveInterval 最大活动时间
	 */
	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
	}
	
	/**
	 * 当前 Session 是否失效
	 *
	 * @return  true: 失效,false: 有效
	 */
	public boolean isInvalid(){
		int intervalTime = (int)(System.currentTimeMillis() - lastTimeillis);
		return intervalTime > maxInactiveInterval;
		
	}
}
