package org.voovan.http.server;

import org.voovan.http.monitor.Monitor;
import org.voovan.http.server.websocket.WebSocketBizHandler;
import org.voovan.http.server.websocket.WebSocketDispatcher;
import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.text.ParseException;

/**
 * HttpServer 对象
 * 
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpServer {
	private AioServerSocket		aioServerSocket;
	private HttpDispatcher	httpDispatcher;
	private WebSocketDispatcher webSocketDispatcher;
	private SessionManager sessionManager;
	private WebServerConfig config;

	/**
	 * 构造函数
	 * 
	 * @param config
	 * @throws IOException
	 *             异常
	 */
	public HttpServer(WebServerConfig config) throws IOException {
		this.config = config;

		// 准备 socket 监听
		aioServerSocket = new AioServerSocket(config.getHost(), config.getPort(), config.getTimeout()*1000);

		//构造 SessionManage
		sessionManager = SessionManager.newInstance(config);

		//请求派发器创建
		this.httpDispatcher = new HttpDispatcher(config,sessionManager);

		this.webSocketDispatcher = new WebSocketDispatcher(config);

		//确认是否启用 HTTPS 支持
		if(config.getCertificateFile()!=null) {
			SSLManager sslManager = new SSLManager("TLS", false);
			sslManager.loadCertificate(System.getProperty("user.dir") + config.getCertificateFile(),
					config.getCertificatePassword(), config.getKeyPassword());
			aioServerSocket.setSSLManager(sslManager);
		}

		aioServerSocket.handler(new HttpServerHandler(config, httpDispatcher,webSocketDispatcher));
		aioServerSocket.filterChain().add(new HttpServerFilter());
		aioServerSocket.messageSplitter(new HttpMessageSplitter());

		//初始化并安装监控功能
		if(config.isMonitor()){
			Monitor.installMonitor(this);
		}
	}

	/**
	 * 获取配置对象
	 * @return
     */
	public WebServerConfig getWebServerConfig() {
		return config;
	}

	/**
	 * 以下是一些 HTTP 方法的成员函数
	 */

	public HttpServer get(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("GET", routeRegexPath, handler);
		return this;
	}

	public HttpServer post(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("POST", routeRegexPath, handler);
		return this;
	}

	public HttpServer head(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("HEAD", routeRegexPath, handler);
		return this;
	}

	public HttpServer put(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("PUT", routeRegexPath, handler);
		return this;
	}

	public HttpServer delete(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("delete", routeRegexPath, handler);
		return this;
	}

	public HttpServer trace(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("TRACE", routeRegexPath, handler);
		return this;
	}

	public HttpServer connect(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("CONNECT", routeRegexPath, handler);
		return this;
	}

	public HttpServer options(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("OPTIONS", routeRegexPath, handler);
		return this;
	}

	public HttpServer otherMethod(String method, String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteMethod(method);
		httpDispatcher.addRouteHandler(method, routeRegexPath, handler);
		return this;
	}
	
	public void socket(String routeRegexPath, WebSocketBizHandler handler) {
		webSocketDispatcher.addRouteHandler(routeRegexPath, handler);
	}
	

	/**
	 * 构建新的 HttpServer,从配置文件读取配置
	 * @param port  HTTP 服务的端口号
	 * @return
	 */
	public static HttpServer newInstance(Integer port) {
		try {
			WebServerConfig config = WebContext.getWebServerConfig();
			if(port!=null) {
				config.setPort(port);
			}
			return new HttpServer(config);
		} catch (IOException e) {
			Logger.error("Create HttpServer failed.",e);
		}
		return null;
	}

	/**
	 * 构建新的 HttpServer,从配置文件读取配置
	 *
	 * @return
	 */
	public static HttpServer newInstance() {
		return newInstance(null);
	}

	/**
	 * 启动服务
	 */
	public HttpServer serve() {
		try {
			WebContext.welcome(config);
			aioServerSocket.start();
		} catch (IOException e) {
			Logger.error("Start HTTP server error.",e);
		}
		return this;
	}
}
