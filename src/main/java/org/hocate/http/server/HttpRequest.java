package org.hocate.http.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hocate.http.message.Request;
import org.hocate.http.message.packet.Cookie;

public class HttpRequest extends Request {

	private HttpSession session;
	private String remoteAddres;
	private int remotePort;
	private String characterSet;
	private Map<String, String> parameters;
	
	protected HttpRequest(Request request,String characterSet){
		super(request);
		this.characterSet=characterSet;
		parameters = new HashMap<String, String>();
		parseParameters();
	}
	
	/**
	 * 根据 Cookie 名称取 Cookie
	 * @param name
	 * @return
	 */
	public Cookie getCookie(String name){
		for(Cookie cookie : this.cookies()){
			if(cookie.getName().equals(name)){
				return cookie;
			}
		}
		return null;
	}

	/**
	 * 获取 Session
	 * @return
	 */
	public HttpSession getSession() {
		return session;
	}

	/**
	 * 设置一个 Session
	 * @param session
	 */
	protected void setSession(HttpSession session) {
		this.session = session;
	}

	/**
	 * 获取对端连接的 IP
	 * @return
	 */
	public String getRemoteAddres() {
		String xForwardedFor = header().get("X-Forwarded-For");
		if(xForwardedFor==null){
			return remoteAddres;
		}else{
			return xForwardedFor.split(",")[0].trim();
		}
	}

	/**
	 * 设置对端连接的 IP
	 * @param remoteAddres
	 */
	protected void setRemoteAddres(String remoteAddres) {
		this.remoteAddres = remoteAddres;
	}

	/**
	 * 获取对端连接的端口
	 * @return
	 */
	public int getRemotePort() {
		return remotePort;
	}

	/**
	 * 设置对端连接的端口
	 * @param port
	 */
	protected void setRemotePort(int port) {
		this.remotePort = port;
	}

	/**
	 * 获取当前默认字符集
	 * @return
	 */
	public String getCharacterSet() {
		return characterSet;
	}

	/**
	 * 设置当前默认字符集
	 * @param charset
	 */
	public void setCharacterSet(String charset) {
		this.characterSet = charset;
	}
	
	/**
	 * 获取请求字符串
	 * @return
	 */
	public String getQueryString(){
		return getQueryString(characterSet);
	}
	
	/**
	 * 获取请求变量集合
	 * @return
	 */
	protected Map<String, String> getParameters() {
		return parameters;
	}
	
	/**
	 * 获取请求变量
	 * @param paramName
	 * @return
	 */
	public String getParameter(String paramName){
		return parameters.get(paramName);
	}
	
	/**
	 * 获取请求变量
	 * @param paramName
	 * @return
	 */
	public List<String> getParameterNames(){
		return Arrays.asList(parameters.keySet().toArray(new String[]{}));
	}
	
	/**
	 * 解析请求参数
	 */
	private void  parseParameters() {
		if(getQueryString()!=null){
			String[] parameterEquals = getQueryString().split("&");
			for(String parameterEqual :parameterEquals){
				int equalFlagPos = parameterEqual.indexOf("=");
				if(equalFlagPos>0){
					String name = parameterEqual.substring(0, equalFlagPos);
					String value = parameterEqual.substring(equalFlagPos+1, parameterEqual.length());
					parameters.put(name, value);
				}else{
					parameters.put(parameterEqual, null);
				}
			}
		}
	}
}
