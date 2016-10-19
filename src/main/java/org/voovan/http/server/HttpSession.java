package org.voovan.http.server;

import org.voovan.http.server.context.WebServerConfig;

import java.util.Map;
import java.util.UUID;
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
	
	/**
	 * 构造函数
	 *
	 * @param config  WEB服务配置对象
	 */
	public HttpSession(WebServerConfig config){
		attributes = new ConcurrentHashMap<String, Object>();
		//生成一个随机的 ID 用作唯一标识
		this.id = UUID.randomUUID().toString().toUpperCase().replaceAll("-", "");
		lastTimeillis = System.currentTimeMillis();
		int sessionTimeout = config.getSessionTimeout();
		this.maxInactiveInterval = sessionTimeout*60*1000;
		
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
	public Object getAttributes(String name) {
		return attributes.get(name);
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
