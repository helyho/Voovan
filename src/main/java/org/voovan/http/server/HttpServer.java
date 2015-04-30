package org.voovan.http.server;

import java.io.IOException;

import org.voovan.http.server.websocket.WebSocketBizHandler;
import org.voovan.http.server.websocket.WebSocketDispatcher;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.messageparter.HttpMessageParter;

/**
 * HttpServer 对象
 * 
 * @author helyho
 * 
 */
public class HttpServer {
	private AioServerSocket		aioServerSocket;
	private HttpDispatcher	httpDispatcher;
	private WebSocketDispatcher webSocketDispatcher;
	private SessionManager sessionManager;

	/**
	 * 构造函数
	 * 
	 * @param host
	 *            监听地址
	 * @param port
	 *            监听端口
	 * @param timeout
	 *            超时时间
	 * @param rootDir
	 *            根目录
	 * @throws IOException
	 *             异常
	 */
	public HttpServer(WebServerConfig config) throws IOException {

		// 准备 socket 监听
		aioServerSocket = new AioServerSocket(config.getHost(), config.getPort(), config.getTimeout());
		
		//构造 SessionManage
		sessionManager = SessionManager.newInstance(config);
		
		//请求派发器创建
		this.httpDispatcher = new HttpDispatcher(config,sessionManager);
		
		this.webSocketDispatcher = new WebSocketDispatcher(config);
		aioServerSocket.handler(new HttpServerHandler(config, httpDispatcher,webSocketDispatcher));
		aioServerSocket.filterChain().add(new HttpServerFilter());
		aioServerSocket.messageParter(new HttpMessageParter());
	}

	/**
	 * 以下是一些 HTTP 方法的成员函数
	 */

	public void get(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("GET", "^" + routeRegexPath + "$", handler);
	}

	public void post(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("POST", "^" + routeRegexPath + "$", handler);
	}

	public void head(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("HEAD", "^" + routeRegexPath + "$", handler);
	}

	public void put(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("PUT", "^" + routeRegexPath + "$", handler);
	}

	public void delete(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("delete", "^" + routeRegexPath + "$", handler);
	}

	public void trace(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("TRACE", "^" + routeRegexPath + "$", handler);
	}

	public void connect(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("CONNECT", "^" + routeRegexPath + "$", handler);
	}

	public void options(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("OPTIONS", "^" + routeRegexPath + "$", handler);
	}

	public void otherMethod(String method, String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteMethod(method);
		httpDispatcher.addRouteHandler(method, "^" + routeRegexPath + "$", handler);
	}
	
	public void socket(String routeRegexPath, WebSocketBizHandler handler) {
		webSocketDispatcher.addRouteHandler(routeRegexPath, handler);
	}
	

	/**
	 * 构建新的 HttpServer,从配置文件读取配置
	 * 
	 * @return
	 */
	public static HttpServer newInstance() {
		try {
			return new HttpServer(WebContext.getWebServerConfig());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 启动服务
	 * 
	 * @throws IOException
	 */
	public void Serve() throws IOException {
		aioServerSocket.start();
	}
}
