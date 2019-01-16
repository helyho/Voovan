package org.voovan.http.message.packet;

import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * HTTP 响应的协议对象
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ResponseProtocol extends Protocol {
	private static ThreadLocal<StringBuilder> THREAD_STRING_BUILDER = ThreadLocal.withInitial(()->new StringBuilder(512));

	/**
	 * 状态代码
	 */
	private int status;

	/**
	 * 状态说明
	 */
	private String statusCode;

	/**
	 * 构造函数
	 */
	public ResponseProtocol(){
		this.status = 200;
		this.statusCode = "OK";
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(String statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * 清理
	 */
	public void clear(){
		super.clear();
		this.status = 200;
		this.statusCode = "OK";
	}

	@Override
	public String toString(){
		StringBuilder stringBuilder = THREAD_STRING_BUILDER.get();
		stringBuilder.setLength(0);
		stringBuilder.append(this.protocol).append("/").append(this.version).append(" ").append(this.status).append(" ").append(this.statusCode).append("\r\n");
		return stringBuilder.toString();
	}
}
