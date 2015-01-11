package org.hocate.http.message.packet;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * HTTP 请求的协议对象
 * @author helyho
 *
 */
public class RequestProtocol extends Protocol {
	
	/**
	 * Http 方法
	 */
	private String method;
	
	/**
	 * 路径
	 */
	private String path;
	
	/**
	 * 请求参数
	 */
	private String queryString;
	
	/**
	 * 构造函数
	 */
	public RequestProtocol(){
		super();
		this.method = "GET";
		this.path = "/";
		this.queryString = "";
	}
		

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getQueryString() {
		return queryString;
	}


	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}


	@Override
	public String toString(){
		try {
			return this.method+" "+this.path+URLEncoder.encode(queryString,"UTF-8")+" "+this.protocol+"/"+this.version+"\r\n";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}
