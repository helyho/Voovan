package org.voovan.http.server;

import org.voovan.http.server.context.HttpModuleConfig;
import org.voovan.http.server.context.HttpRouterConfig;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.websocket.WebSocketRouter;
import org.voovan.http.server.websocket.WebSocketDispatcher;
import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.IOException;

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
	private AioServerSocket	aioServerSocket;
	private HttpDispatcher	httpDispatcher;
	private WebSocketDispatcher webSocketDispatcher;
	private SessionManager sessionManager;
	private WebServerConfig config;

	/**
	 * 构造函数
	 * 
	 * @param config  WEB 配对对象
	 * @throws IOException
	 *             异常
	 */
	public HttpServer(WebServerConfig config) throws IOException {
		this.config = config;

		//[Socket] 准备 socket 监听
		aioServerSocket = new AioServerSocket(config.getHost(), config.getPort(), config.getTimeout()*1000);

		//[HTTP] 构造 SessionManage
		sessionManager = SessionManager.newInstance(config);

		//[HTTP]请求派发器创建
		this.httpDispatcher = new HttpDispatcher(config,sessionManager);

		this.webSocketDispatcher = new WebSocketDispatcher(config);

		//[Socket]确认是否启用 HTTPS 支持
		if(config.isHttps()) {
			SSLManager sslManager = new SSLManager("TLS", false);
			sslManager.loadCertificate(System.getProperty("user.dir") + config.getHttps().getCertificateFile(),
					config.getHttps().getCertificatePassword(), config.getHttps().getKeyPassword());
			aioServerSocket.setSSLManager(sslManager);
		}

		aioServerSocket.handler(new HttpServerHandler(config, httpDispatcher,webSocketDispatcher));
		aioServerSocket.filterChain().add(new HttpServerFilter());
		aioServerSocket.messageSplitter(new HttpMessageSplitter());
	}

	/**
	 * 将配置文件中的 Router 配置载入到 HttpServer
     */
	private void  initConfigedRouter(){
		for(HttpRouterConfig httpRouterConfig : config.getRouterConfigs()){
			String method = httpRouterConfig.getMethod();
			String route = httpRouterConfig.getRoute();
			String className = httpRouterConfig.getClassName();
			otherMethod(method,route,httpRouterConfig.getHttpRouterInstance());
		}
	}

	/**
	 * 模块安装
     */
	public void initModule() {
		for (HttpModuleConfig httpModuleConfig : config.getModuleonfigs()) {
			HttpModule httpModule = httpModuleConfig.getHttpModuleInstance(this);
			if(httpModule!=null){
				httpModule.install();
			}
		}
	}


	/**
	 * 获取 Http 服务配置对象
	 * @return 返回 Http 服务配置对象
     */
	public WebServerConfig getWebServerConfig() {
		return config;
	}

	/**
	 * 以下是一些 HTTP 方法的成员函数
	 */

	/**
	 * GET 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
     * @return HttpServer对象
     */
	public HttpServer get(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("GET", routeRegexPath, router);
		return this;
	}

	/**
	 * POST 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
     */
	public HttpServer post(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("POST", routeRegexPath, router);
		return this;
	}

	/**
	 * HEAD 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
	 */
	public HttpServer head(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("HEAD", routeRegexPath, router);
		return this;
	}

	/**
	 * PUT 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
	 */
	public HttpServer put(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("PUT", routeRegexPath, router);
		return this;
	}

	/**
	 * DELETE 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
	 */
	public HttpServer delete(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("DELETE", routeRegexPath, router);
		return this;
	}

	/**
	 * TRACE 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
	 */
	public HttpServer trace(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("TRACE", routeRegexPath, router);
		return this;
	}

	/**
	 * CONNECT 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
	 */
	public HttpServer connect(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("CONNECT", routeRegexPath, router);
		return this;
	}

	/**
	 * OPTIONS 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
	 */
	public HttpServer options(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("OPTIONS", routeRegexPath, router);
		return this;
	}

	/**
	 * 其他请求
	 * @param method 请求方法
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return HttpServer对象
	 */
	public HttpServer otherMethod(String method, String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteMethod(method);
		httpDispatcher.addRouteHandler(method, routeRegexPath, router);
		return this;
	}

	/**
	 * WebSocket 服务
	 * @param routeRegexPath 匹配路径
	 * @param router WebSocket处理句柄
	 * @return HttpServer对象
     */
	public HttpServer socket(String routeRegexPath, WebSocketRouter router) {
		webSocketDispatcher.addRouteHandler(routeRegexPath, router);
		return this;
	}

	/**
	 * 构建新的 HttpServer,从配置对象读取配置
	 * @param config  WebServer配置类
	 * @return HttpServer 对象
	 */
	public static HttpServer newInstance(WebServerConfig config) {

		try {
			if(config!=null) {
				return new HttpServer(config);
			}else{
				Logger.error("Create HttpServer failed: WebServerConfig object is null.");
			}
		} catch (IOException e) {
			Logger.error("Create HttpServer failed.",e);
		}

		return null;
	}

	/**
	 * 构建新的 HttpServer,指定服务端口
	 * @param port  HTTP 服务的端口号
	 * @return HttpServer 对象
	 */
	public static HttpServer newInstance(int port) {
		WebServerConfig config = WebContext.getWebServerConfig();
		config.setPort(port);
		return newInstance(config);
	}

	/**
	 * 构建新的 HttpServer,从配置文件读取配置
	 *
	 * @return HttpServer 对象
	 */
	public static HttpServer newInstance() {
		return newInstance(WebContext.getWebServerConfig());
	}

	/**
	 * 读取Classes目录和lib目录中的class或者jar文件
	 */
	private static void loadContextBin(){
		try {
			TEnv.loadBinary(TEnv.getSystemPath("classes"));
			TEnv.loadJars(TEnv.getSystemPath("lib"));
		} catch (NoSuchMethodException | IOException | SecurityException e) {
			Logger.error("Voovan WEBServer Loader ./classes or ./lib error." ,e);
		}
	}

	/**
	 * 启动服务
	 *
	 * @return HttpServer 对象
	 */
	public HttpServer serve() {
		try {
			//输出欢迎信息
			WebContext.welcome(config);
			WebContext.initWebServerPlugin();

			loadContextBin();
			initConfigedRouter();
			initModule();
			Logger.simple("Process ID: "+ TEnv.getCurrentPID());
			Logger.simple("WebServer working on: http"+(config.isHttps()?"s":"")+"://"+config.getHost()+":"+config.getPort()+" ...");
			aioServerSocket.start();
		} catch (IOException e) {
			Logger.error("Start HTTP server error.",e);
		}
		return this;
	}

	/**
	 * 启动 HttpServer 服务
	 * @param args 启动参数
	 */
	public static void main(String[] args) {
		 WebServerConfig config = null;
		if(args.length>0){
			for(int i=0;i<args.length;i++){
				//服务端口
				if(args[i].equals("-p")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setPort(Integer.parseInt(args[i]));
				}

				//连接超时时间(s)
				if(args[i].equals("-t")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setTimeout(Integer.parseInt(args[i]));
				}

				//上下文路径
				if(args[i].equals("-cp")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setContextPath(args[i]);
				}

				//首页索引文件的名称,默认index.htm,index.html,default.htm,default.htm
				if(args[i].equals("-i")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setIndexFiles(args[i]);
				}

				//匹配路由不区分大小写,默认是 false
				if(args[i].equals("-mi")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setMatchRouteIgnoreCase(true);
				}

				//默认字符集,默认 UTF-8
				if(args[i].equals("-c")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setCharacterSet(args[i]);
				}

				//是否启用Gzip压缩,默认 true
				if(args[i].equals("-noGzip")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setGzip(false);
				}

				//是否记录access.log,默认 true
				if(args[i].equals("-noAccessLog")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setAccessLog(false);
				}

				//HTTPS 证书
				if(args[i].equals("-https.CertificateFile")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setCertificateFile(args[i]);
				}
				//证书密码
				if(args[i].equals("-https.CertificatePassword")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setCertificatePassword(args[i]);
				}
				//证书Key 密码
				if(args[i].equals("-https.KeyPassword")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setKeyPassword(args[i]);
				}

				//输出版本号
				if(args[i].equals("-v")){
					Logger.simple("Version:"+WebContext.getVERSION());
					return;
				}

				if(args[i].equals("-help") || args[i].equals("-h") || args[i].equals("-?")){
					Logger.simple("Usage: java -jar voovan-framework.jar [Options]");
					Logger.simple("Start voovan webserver");
					Logger.simple("");
					Logger.simple("Options:");
					Logger.simple("  -p \t\t\t\t\t\t\t Webserver bind port number");
					Logger.simple("  -t \t\t\t\t\t\t\t Socket timeout");
					Logger.simple("  -cp \t\t\t\t\t\t Context path, contain webserver static file");
					Logger.simple("  -i \t\t\t\t\t\t\t index file for client access to webserver");
					Logger.simple("  -mi \t\t\t\t\t\t Match route ignore case");
					Logger.simple("  -c \t\t\t\t\t\t\t set default charset");
					Logger.simple("  -noGzip \t\t\t\t\t Do not use gzip for client");
					Logger.simple("  -noAccessLog \t\t\t\t Do not write access log to access.log");
					Logger.simple("  -https.CertificateFile \t\t Certificate file for https");
					Logger.simple("  -https.CertificatePassword \t Certificate file for https");
					Logger.simple("  -https.KeyPassword \t\t\t Certificate file for https");
					Logger.simple("  -help \t\t\t\t\t\t how to use this command");
					Logger.simple("  -v \t\t\t\t\t\t\t Show the version information");
					return;
				}
			}
		}
		config = config==null?WebContext.getWebServerConfig():config;

		HttpServer httpServer = HttpServer.newInstance(config);

		httpServer.serve();
	}
}
