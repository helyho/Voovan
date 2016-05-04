package org.voovan.http.server;

import org.voovan.http.message.Response;
import org.voovan.tools.TDateTime;

import java.util.Date;

/**
 * HTTPServer 响应对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpResponse extends Response {
	private String	characterSet;

	/**
	 * 构造 HTTP 响应对象
	 * @param response     响应对象
	 * @param characterSet 字符集
     */
	protected HttpResponse(Response response,String characterSet) {
		super(response);
		this.characterSet=characterSet;
		//设置当前响应的时间
		this.header().put("Date",TDateTime.formatToGMT(new Date()));
	}

	/**
	 * 获取当前默认字符集
	 * 
	 * @return 默认字符集
	 */
	public String getCharacterSet() {
		return characterSet;
	}

	/**
	 * 设置当前默认字符集
	 * 
	 * @param characterSet 默认字符集
	 */
	public void setCharacterSet(String characterSet) {
		this.characterSet = characterSet;
	}

	/**
	 * 写入一个 byte 数组
	 * 
	 * @param bytes  byte 数组
	 */
	public void write(byte[] bytes) {
		body().write(bytes);
	}

	/**
	 * 写入一个 byte 数组
	 * 
	 * @param bytes  byte 数组
	 * @param offset 偏移量
	 * @param length 写入长度
	 */
	public void write(byte[] bytes, int offset, int length) {
		body().write(bytes, offset, length);
	}

	/**
	 * 写入一个字符串
	 * 
	 * @param strs 字符串
	 */
	public void write(String strs) {
		if(strs!=null){
			body().write(strs, characterSet);
		}
	}
	
	/**
	 * 清理报文
	 */
	public void clear() {
		body().clear();
	}
	
	/**
	 * 重定向
	 * @param path 重定向路径
	 */
	public void redirct(String path){
		protocol().setStatus(302);
		protocol().setStatusCode("Moved Permanently");
		header().put("Location", path);
		clear();
	}
}
