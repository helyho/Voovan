package org.voovan.http.message.packet;

/**
 * HTTP 响应的协议对象
 * @author helyho
 *
 */
public class ResponseProtocol extends Protocol {
	
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

	@Override
	public String toString(){
		return this.protocol+"/"+this.version+" "+this.status+" "+this.statusCode+"\r\n";
	}
}
