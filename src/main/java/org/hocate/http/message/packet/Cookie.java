package org.hocate.http.message.packet;

import java.util.Map;
import java.util.Map.Entry;

/**
 *  HTTP 的 cookie 对象
 * @author helyho
 *
 */
public class Cookie {
	private String domain;
	private String path;
	private String expires;
	private int maxAge = -999999;
	private boolean secure;
	private boolean httpOnly;
	
	private String name;
	private String value;
	
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
	public String getExpires() {
		return expires;
	}
	public void setExpires(String expires) {
		this.expires = expires;
	}
	public int getMaxage() {
		return maxAge;
	}
	public void setMaxage(int maxAge) {
		this.maxAge = maxAge;
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
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	@Override
	public String toString(){
		return (this.name!=null||this.value!=null? (this.name+"="+this.value) : "")+
				(this.domain!=null ? ("; domain="+this.domain) : "")+
				(this.expires!=null ? ("; expires="+this.expires) : "")+
				(this.maxAge!=-999999 ? ("; max-age="+this.maxAge) : "")+
				(this.path!=null ? ("; path="+this.path) : "")+
				(this.httpOnly?"httponly; ":"")+(this.secure?"secure":"");
	}
	
	/**
	 * 通过 Map 构建一个 Cookie 对象
	 * @param cookieMap
	 * @return
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
			case "expires" :
				cookie.setExpires(cookieMapItem.getValue());
				break;
			case "max-age" :
				cookie.setMaxage(Integer.valueOf(cookieMapItem.getValue()));
				break;
			case "secure" :
				cookie.setSecure(true);
				break;
			case "httponly" :
				cookie.setHttpOnly(true);
				break;
			default:
				cookie.setName(cookieMapItem.getKey());
				cookie.setValue(cookieMapItem.getValue());
				break;
			}
		}
		return cookie;
	}
}
