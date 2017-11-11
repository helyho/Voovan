package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.network.IoSession;
import org.voovan.tools.TString;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.reflect.annotation.NotSerialization;

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
	private volatile long lastTimeillis;
	@NotSerialization
	private SessionManager sessionManager;
	@NotSerialization
	private IoSession socketSession;
	@NotSerialization
	private WebSocketSession webSocketSession;

	private boolean needSave;
	private boolean isAutoCleanRun;

	@NotSerialization
	private CleanTask cleanTask= null;


	/**
	 * 构造函数
	 *
	 * @param config  WEB服务配置对象
	 * @param sessionManager Session管理器
	 * @param socketSession   Socket会话对象
	 */
	public HttpSession(WebServerConfig config, SessionManager sessionManager, IoSession socketSession){
		// ID的创建转义到 save 方法中.在保存时才创建 ID
		attributes = new ConcurrentHashMap<String, Object>();
		lastTimeillis = System.currentTimeMillis();
		int sessionTimeout = config.getSessionTimeout();
		if(sessionTimeout<=0){
			sessionTimeout = 30;
		}
		this.maxInactiveInterval = sessionTimeout*60*1000;
		this.sessionManager = sessionManager;
		this.socketSession = socketSession;

		needSave = false;
		isAutoCleanRun = false;
	}

	private void autoClean(){
		if(!isAutoCleanRun) {
			//如果 session 可以自清除则不进行清理
			if (!sessionManager.autoExpire()) {
				cleanTask = new CleanTask(sessionManager, id);

				Global.getHashWheelTimer().addTask(cleanTask, maxInactiveInterval / 1000);
			}
			isAutoCleanRun = true;
		}
	}

	private class CleanTask extends HashWheelTask {

		@NotSerialization
		private String sessionId;
		private SessionManager sessionManager;

		public CleanTask(SessionManager sessionManager, String sessionId) {
			this.sessionId = sessionId;
			this.sessionManager = sessionManager;
		}

		@Override
		public void run() {
			HttpSession session = sessionManager.getSession(sessionId);
			if (session!=null && session.isExpire()) {
				session.removeFromSessionManager();
				this.cancel();
			}
		}
	};

	/**
	 * 用于从会话池中取出的会话实例化
	 * @param sessionManager Session管理器
	 * @param socketSession Socket会话对象
	 */
	public void init(SessionManager sessionManager, IoSession socketSession){
		this.sessionManager = sessionManager;
		this.socketSession = socketSession;
		refresh();
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
		System.out.println("Refresh to " +lastTimeillis);
		needSave = true;
		save();
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
		needSave = true;
	}

	/**
	 *  删除当前 Session 属性
	 * @param name	属性名
	 */
	public void removeAttribute(String name) {
		attributes.remove(name);
		needSave = true;
	}

	/**
	 *  返回当前 Session 的属性Map
	 */
	public Map<String,Object> attribute() {
		return attributes;
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
		if(cleanTask!=null){
			cleanTask.cancel();
		}
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
	 * @return 最大活动时间, 单位: 毫秒
	 */
	public int getMaxInactiveInterval() {
		return maxInactiveInterval;
	}

	/**
	 * 设置最大活动时间
	 *
	 * @param maxInactiveInterval 最大活动时间, 单位: 毫秒
	 */
	public void setMaxInactiveInterval(int maxInactiveInterval) {
		this.maxInactiveInterval = maxInactiveInterval;
		needSave = true;
	}

	/**
	 * 当前 Session 是否失效
	 *
	 * @return  true: 失效,false: 有效
	 */
	public boolean isExpire(){
		int intervalTime = (int)(System.currentTimeMillis() - lastTimeillis);
		System.out.println("test to " +lastTimeillis + " "+ intervalTime + " " + (intervalTime > maxInactiveInterval));
		return intervalTime > maxInactiveInterval;
	}

	/**
	 * 保存 Session
	 */
	public void save(){
		if(sessionManager!=null && needSave) {
			if(id==null){
				//生成一个随机的 ID 用作唯一标识
				this.id = TString.generateId(this);
			}
			sessionManager.saveSession(this);
			autoClean();
			needSave = false;
		}
	}

	/**
	 * 关闭会话对象
	 */
	public void close(){
		socketSession.close();
	}
}
