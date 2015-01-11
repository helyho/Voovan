package org.hocate.http.message.packet;

/**
 * HTTP 的协议对象,报文的第一行
 * @author helyho
 *
 */
public class Protocol {
	/**
	 * 协议名称
	 */
	protected String protocol;
	
	/**
	 * 版本
	 */
	protected float version;
	
	/**
	 * 构造函数
	 */
	public Protocol(){
		this.protocol = "HTTP";
		this.version = 1.1F;
	}
	
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}


	public float getVersion() {
		return version;
	}

	public void setVersion(float version) {
		this.version = version;
	}
}
