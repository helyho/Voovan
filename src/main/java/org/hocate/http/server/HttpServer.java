package org.hocate.http.server;

import java.io.IOException;

import org.hocate.network.aio.AioServerSocket;
import org.hocate.network.messageParter.HttpMessageParter;

/**
 * HttpServer 对象
 * 
 * @author helyho
 * 
 */
public class HttpServer {
	private AioServerSocket		aioServerSocket;
	private RequestDispatcher	requestDispatcher;

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
	public HttpServer(WebConfig config) throws IOException {

		// 准备 socket 监听
		aioServerSocket = new AioServerSocket(config.getHost(), config.getPort(), config.getTimeout());
		this.requestDispatcher = new RequestDispatcher(config);
		aioServerSocket.handler(new HttpServerHandler(config, requestDispatcher));
		aioServerSocket.filterChain().add(new HttpServerFilter());
		aioServerSocket.messageParter(new HttpMessageParter());
	}

	/**
	 * 以下是一些 HTTP 方法的成员函数
	 */

	public void get(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("GET", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void post(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("POST", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void head(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("HEAD", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void put(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("PUT", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void delete(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("delete", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void trace(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("TRACE", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void connect(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("CONNECT", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void options(String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteRuler("OPTIONS", "^" + routeRegexPath + "$", routeBuiz);
	}

	public void otherMethod(String method, String routeRegexPath, Router routeBuiz) {
		requestDispatcher.addRouteMethod(method);
		requestDispatcher.addRouteRuler(method, "^" + routeRegexPath + "$", routeBuiz);
	}

	/**
	 * 构建新的 HttpServer,从配置文件读取配置
	 * 
	 * @return
	 */
	public static HttpServer newInstance() {
		try {
			return new HttpServer(WebContext.getWebConfig());
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
