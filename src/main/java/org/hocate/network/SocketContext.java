package org.hocate.network;

/**
 * socket 上下文
 * @author helyho
 *
 */
public abstract class SocketContext {
	protected String host;
	protected int port;
	protected int readTimeout;
	
	protected IoHandler handler;
	protected Chain<IoFilter> filterChain;
	protected MessageParter messageParter;
	protected SSLManager sslManager;
	
	
	/**
	 * 构造函数
	 */
	public SocketContext(String host,int port,int readTimeout) {
		this.host = host;
		this.port = port;
		this.readTimeout = readTimeout;
		filterChain = new Chain<IoFilter>();
	}
	
	/**
	 * 克隆对象
	 * @param parentSocketContext
	 */
	public void cloneInit(SocketContext parentSocketContext){
		this.readTimeout = parentSocketContext.readTimeout;
		this.handler = parentSocketContext.handler;
		this.filterChain = parentSocketContext.filterChain;
		this.messageParter = parentSocketContext.messageParter;
		this.sslManager = parentSocketContext.sslManager;
	}
	
	/**
	 * 无参数构造函数
	 */
	public SocketContext() {
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

	public void setHost(String host) {
		if(!isConnect()){
			this.host = host;
		}else{
			System.out.println("Socket is Open,can't set host!");
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		if(!isConnect()){
			this.port = port;
		}else{
			System.out.println("Socket is Open,can't set port!");
		}
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
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
	 * 获取消息分割器
	 * @return
	 */
	public MessageParter messageParter() {
		return this.messageParter;
	}
	
	/**
	 * 设置消息分割器
	 * @return
	 */
	public void messageParter(MessageParter messageParter) {
		this.messageParter = messageParter;
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
