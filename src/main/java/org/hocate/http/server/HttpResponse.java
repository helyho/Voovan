package org.hocate.http.server;

import org.hocate.http.message.Response;

public class HttpResponse extends Response {
	private String	characterSet;

	protected HttpResponse(Response response) {
		super(response);
	}

	/**
	 * 获取当前默认字符集
	 * 
	 * @return
	 */
	public String getCharacterSet() {
		return characterSet;
	}

	/**
	 * 设置当前默认字符集
	 * 
	 * @param charset
	 */
	public void setCharacterSet(String characterSet) {
		this.characterSet = characterSet;
	}

	/**
	 * 写入一个 byte 数组
	 * 
	 * @param bytes
	 */
	public void write(byte[] bytes) {
		body().write(bytes);
	}

	/**
	 * 写入一个 byte 数组
	 * 
	 * @param bytes
	 * @param offset
	 * @param length
	 */
	public void write(byte[] bytes, int offset, int length) {
		body().write(bytes, offset, length);
	}

	/**
	 * 写入一个字符串
	 * 
	 * @param bytes
	 */
	public void write(String strs) {
		if(strs!=null){
			body().write(strs, characterSet);
		}
	}
	
	/**
	 * 写入一个 byte 数组
	 * 
	 * @param bytes
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
