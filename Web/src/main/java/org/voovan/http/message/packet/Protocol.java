package org.voovan.http.message.packet;

/**
 * HTTP 的协议对象,报文的第一行
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Protocol {
	/**
	 * 协议名称
	 */
	protected String protocol;
	
	/**
	 * 版本
	 */
	protected String version;
	
	/**
	 * 构造函数
	 */
	public Protocol(){
		this.protocol = "HTTP";
		this.version = "1.1";
	}
	
	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}


	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * 清理
	 */
	public void clear(){
		this.protocol = "HTTP";
		this.version = "1.1";
	}
}
