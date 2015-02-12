package org.hocate.http.server;

public class WebConfig {
	private String host;
	private int port;
	private int timeout;
	private String contextPath;
	private String characterSet;
	private String sessionContainer;
	private int sessionTimeout;
	private int keepAliveTimeout;
	
	protected void setHost(String host) {
		this.host = host;
	}
	protected void setPort(int port) {
		this.port = port;
	}
	protected void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	protected void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
	protected void setCharacterSet(String characterSet) {
		this.characterSet = characterSet;
	}
	protected void setSessionContainer(String sessionContainer) {
		this.sessionContainer = sessionContainer;
	}
	protected void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}
	protected void setKeepAliveTimeout(int keepAliveTimeout) {
		this.keepAliveTimeout = keepAliveTimeout;
	}
	
	public String getHost() {
		return host;
	}
	public int getPort() {
		return port;
	}
	public int getTimeout() {
		return timeout;
	}
	public String getContextPath() {
		return contextPath;
	}
	public String getCharacterSet() {
		return characterSet;
	}
	public String getSessionContainer() {
		return sessionContainer;
	}
	public int getSessionTimeout() {
		return sessionTimeout;
	}
	public int getKeepAliveTimeout() {
		return keepAliveTimeout;
	}
	
	
}
