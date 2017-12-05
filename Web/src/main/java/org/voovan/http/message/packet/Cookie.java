package org.voovan.http.message.packet;

import org.voovan.http.message.Request;
import org.voovan.http.server.context.WebContext;
import org.voovan.tools.log.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

/**
 *  HTTP 的 cookie 对象
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Cookie {
	private String domain;
	private String path;
	private int maxAge = -999999;
	private String expires;
	private boolean secure;
	private boolean httpOnly;

	private String name;
	private String value;

	private Cookie(){

	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public String getExpires() {
		return expires;
	}

	public void setExpires(String expires) {
		this.expires = expires;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		try {
			return URLDecoder.decode(this.value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.error(e);
		}
		return this.value;
	}

	public void setValue(String value) {
		try {
			this.value = URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.error(e);
		}

	}

	@Override
	public String toString(){
		return (this.name!=null||this.value!=null? (this.name+"="+this.value) : "")+
				(this.domain!=null ? ("; domain="+this.domain) : "")+
				(this.maxAge!=-999999 ? ("; max-age="+this.maxAge) : "")+
				(this.path!=null ? ("; path="+this.path) : " ")+
				(this.httpOnly?"; httponly; ":"")+(this.secure?"; secure":"");
	}

	/**
	 * 通过 Map 构建一个 Cookie 对象
	 * @param cookieMap Cookie 属性 Map
	 * @return Cookie 对象
	 */
	public static Cookie buildCookie(Map<String, String> cookieMap){
		Cookie cookie = new Cookie();
		for(Entry<String, String> cookieMapItem : cookieMap.entrySet()){
			switch(cookieMapItem.getKey().toLowerCase()){
				case "domain" :
					cookie.setDomain(cookieMapItem.getValue());
					break;
				case "path" :
					cookie.setPath(cookieMapItem.getValue());
					break;
				case "max-age" :
					cookie.setMaxAge(Integer.parseInt(cookieMapItem.getValue()));
					break;
				case "secure" :
					cookie.setSecure(true);
					break;
				case "httponly" :
					cookie.setHttpOnly(true);
					break;
				case "expires" :
					cookie.setExpires(cookieMapItem.getValue());
					break;
				default:
					cookie.setName(cookieMapItem.getKey());
					cookie.setValue(cookieMapItem.getValue());
					break;
			}
		}
		return cookie;
	}


	/**
	 * 创建一个 Cookie
	 * @param domain	cookie的受控域
	 * @param path      cookie路径
	 * @param name		名称
	 * @param value		值
	 * @param maxAge	失效时间,单位秒
	 * @return Cookie 对象
	 */
	public static Cookie newInstance(String domain, String path, String name,String value,int maxAge, boolean isHttpOnly){
		Cookie cookie = new Cookie();
		cookie.setName(name);
		cookie.setValue(value);
		cookie.setPath(path);
		cookie.setDomain(domain);
		cookie.setMaxAge(maxAge);
		cookie.setHttpOnly(isHttpOnly);
		return cookie;
	}

	/**
	 * 创建一个 Cookie
	 * @param request	请求对象
	 * @param path      cookie路径
	 * @param name		名称
	 * @param value		值
	 * @param maxAge	失效时间,单位秒
	 * @return Cookie 对象
	 */
	public static Cookie newInstance(Request request, String path, String name, String value, int maxAge, boolean isHttpOnly){
		String Host =  request.header().get("Host");
		String domain = Host.split(":")[0];
		return newInstance(domain , path, name, value, maxAge, isHttpOnly);
	}

	/**
	 * 创建一个 Cookie
	 * @param request	请求对象
	 * @param path      cookie路径
	 * @param name		名称
	 * @param value		值
	 * @return Cookie 对象
	 */
	public static Cookie newInstance(Request request, String path, String name,String value){
		return newInstance(request, path, name, value, WebContext.getWebServerConfig().getSessionTimeout() * 60, false);
	}

	/**
	 * 创建一个 Cookie
	 * @param request	请求对象
	 * @param name		名称
	 * @param value		值
	 * @return Cookie 对象
	 */
	public static Cookie newInstance(Request request,String name,String value){
		return newInstance(request, "/", name, value, WebContext.getWebServerConfig().getSessionTimeout() * 60, false);
	}


}
