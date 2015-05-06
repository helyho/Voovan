package org.voovan.network;

/**
 * socket 上下文
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class SocketContext {
	protected String host;
	protected int port;
	protected int readTimeout;
	
	protected IoHandler handler;
	protected Chain<IoFilter> filterChain;
	protected MessageSplitter messageSplitter;
	protected SSLManager sslManager;
	protected ConnectModel connectModel;
	
	
	/**
	 * 构造函数
	 */
	public SocketContext(String host,int port,int readTimeout) {
		this.host = host;
		this.port = port;
		this.readTimeout = readTimeout;
		connectModel = null;
		filterChain = new Chain<IoFilter>();
	}
	
	/**
	 * 克隆对象
	 * @param parentSocketContext
	 */
	protected void copyFrom(SocketContext parentSocketContext){
		this.readTimeout = parentSocketContext.readTimeout;
		this.handler = parentSocketContext.handler;
		this.filterChain = parentSocketContext.filterChain;
		this.messageSplitter = parentSocketContext.messageSplitter;
		this.sslManager = parentSocketContext.sslManager;
	}
	
	/**
	 * 无参数构造函数
	 */
	protected SocketContext() {
		filterChain = new Chain<IoFilter>();
	}

	public SSLManager getSSLManager() {
		return sslManager;
	}

	public void setSSLManager(SSLManager sslManager) {
		if(this.sslManager==null){
			this.sslManager = sslManager;
		}
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	/**
	 * 获取超时时间
	 * @return
	 */
	public int getReadTimeout() {
		return readTimeout;
	}
	
	/**
	 * 获取连接模式
	 * @return
	 */
	public ConnectModel getConnectModel() {
		return connectModel;
	}

	/**
	 * 获取业务处理句柄
	 * @return 业务处理句柄
	 */
	public IoHandler handler(){
		return this.handler;
	} 
	
	/**
	 * 设置业务处理句柄
	 * @param handler 业务处理句柄
	 */
	public void handler(IoHandler handler){
		this.handler = handler;
	} 
	
	/**
	 * 获取过滤器链
	 * @return 过滤器链
	 */
	public Chain<IoFilter> filterChain(){
		return this.filterChain;
	}
	
	/**
	 * 获取消息粘包分割器
	 * @return
	 */
	public MessageSplitter messageSplitter() {
		return this.messageSplitter;
	}
	
	/**
	 * 设置消息粘包分割器
	 * @return
	 */
	public void messageSplitter(MessageSplitter messageSplitter) {
		this.messageSplitter = messageSplitter;
	}
	
	/**
	 * 启动上下文连接
	 * @return
	 */
	public abstract void start() throws Exception;
	
	/**
	 * 上下文连接是否打开
	 * @return true:连接打开,false:连接关闭
	 */
	public abstract boolean isConnect();
	
	/**
	 * 关闭连接
	 */
	public abstract boolean Close();
}
