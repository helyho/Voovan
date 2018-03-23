package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.server.context.HttpModuleConfig;
import org.voovan.http.server.context.HttpRouterConfig;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.aop.Aop;
import org.voovan.tools.hotswap.Hotswaper;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.Map;

/**
 * WebServer 对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebServer {
	private AioServerSocket aioServerSocket;
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
	public WebServer(WebServerConfig config) throws IOException {
		this.config = config;

		initAop();
		initHotSwap();
		initWebServer(config);
	}

	/**
	 * 返回 Session 管理器
	 * @return
	 */
	public SessionManager getSessionManager(){
		return this.sessionManager;
	}

	/**
	 * 初始化热部署
	 */
	private void initHotSwap() {
		//热加载
		{
			try {
				if(config.getHotSwapInterval() > 0) {
					Hotswaper hotSwaper = new Hotswaper();
					hotSwaper.autoReload(config.getHotSwapInterval());
				}
			} catch (Exception e) {
				Logger.error("Init hotswap failed", e);
			}
		}
	}

	/**
	 * 初始化热部署
	 */
	private void initAop() {
		//热加载
		{
			try {
				Aop.init(config.getScanAopPackage());
			} catch (Exception e) {
				Logger.error("Init aop failed", e);
			}
		}
	}

	private void initWebServer(WebServerConfig config) {
		//[HTTP] 构造 SessionManage
		this.sessionManager = SessionManager.newInstance(config);

		//[HTTP]请求派发器创建
		this.httpDispatcher = new HttpDispatcher(config, sessionManager);

		this.webSocketDispatcher = new WebSocketDispatcher(config, sessionManager);
	}


	private void initSocketServer(WebServerConfig config) throws IOException{

		//[Socket] 准备 socket 监听
		aioServerSocket = new AioServerSocket(config.getHost(), config.getPort(), config.getReadTimeout()*1000, config.getSendTimeout()*1000, 0);

		//[Socket]确认是否启用 HTTPS 支持
		if(config.isHttps()) {
			SSLManager sslManager = new SSLManager("TLS", false);
			sslManager.loadCertificate(System.getProperty("user.dir") + config.getHttps().getCertificateFile(),
					config.getHttps().getCertificatePassword(), config.getHttps().getKeyPassword());
			aioServerSocket.setSSLManager(sslManager);
		}

		aioServerSocket.handler(new WebServerHandler(config, httpDispatcher, webSocketDispatcher));
		aioServerSocket.filterChain().add(new WebServerFilter());
		aioServerSocket.messageSplitter(new HttpMessageSplitter());
	}

	/**
	 * 将配置文件中的 Router 配置载入到 WebServer
	 */
	private void initRouter(){
		for(HttpRouterConfig httpRouterConfig : config.getRouterConfigs()){
			String method = httpRouterConfig.getMethod();
			String route = httpRouterConfig.getRoute();
			String className = httpRouterConfig.getClassName();

			if(!method.equals("WEBSOCKET")) {
				otherMethod(method, route, httpRouterConfig.getHttpRouterInstance());
			}else{
				socket(route, httpRouterConfig.getWebSocketRouterInstance());
			}
		}
	}

	/**
	 * 模块安装
	 */
	public void initModule() {
		for (HttpModuleConfig httpModuleConfig : config.getModuleonfigs()) {
			HttpModule httpModule = httpModuleConfig.getHttpModuleInstance(this);
			if(httpModule!=null){
				httpModule.runModuleInit();
				httpModule.install();
			}

		}
	}

	/**
	 * 模块卸载
	 */
	public void unInitModule() {
		//卸载模块
		for (HttpModuleConfig moduleConfig : this.config.getModuleonfigs().toArray(new HttpModuleConfig[0])) {
			HttpModule httpModule = moduleConfig.getHttpModuleInstance(this);
			httpModule.runModuleDestory();
			httpModule.unInstall();
			Logger.simple("[SYSTEM] Module ["+moduleConfig.getName()+"] uninstall");
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
	 * @return WebServer对象
	 */
	public WebServer get(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("GET", routeRegexPath, router);
		return this;
	}

	/**
	 * POST 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer post(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("POST", routeRegexPath, router);
		return this;
	}

	/**
	 * 同时注册 GET 和 POST 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer getAndPost(String routeRegexPath, HttpRouter router) {
		get(routeRegexPath, router);
		post(routeRegexPath, router);
		return this;
	}

	/**
	 * HEAD 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer head(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("HEAD", routeRegexPath, router);
		return this;
	}

	/**
	 * PUT 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer put(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("PUT", routeRegexPath, router);
		return this;
	}

	/**
	 * DELETE 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer delete(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("DELETE", routeRegexPath, router);
		return this;
	}

	/**
	 * TRACE 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer trace(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("TRACE", routeRegexPath, router);
		return this;
	}

	/**
	 * CONNECT 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer connect(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("CONNECT", routeRegexPath, router);
		return this;
	}

	/**
	 * OPTIONS 请求
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer options(String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteHandler("OPTIONS", routeRegexPath, router);
		return this;
	}

	/**
	 * 其他请求
	 * @param method 请求方法
	 * @param routeRegexPath 匹配路径
	 * @param router  HTTP处理请求句柄
	 * @return WebServer对象
	 */
	public WebServer otherMethod(String method, String routeRegexPath, HttpRouter router) {
		httpDispatcher.addRouteMethod(method);
		httpDispatcher.addRouteHandler(method, routeRegexPath, router);
		return this;
	}

	/**
	 * WebSocket 服务
	 * @param routeRegexPath 匹配路径
	 * @param router WebSocket处理句柄
	 * @return WebServer对象
	 */
	public WebServer socket(String routeRegexPath, WebSocketRouter router) {
		webSocketDispatcher.addRouteHandler(routeRegexPath, router);
		return this;
	}

	/**
	 * 构建新的 WebServer,从配置对象读取配置
	 * @param config  WebServer配置类
	 * @return WebServer 对象
	 */
	public static WebServer newInstance(WebServerConfig config) {

		try {
			if(config!=null) {
				return new WebServer(config);
			}else{
				Logger.error("Create WebServer failed: WebServerConfig object is null.");
			}
		} catch (IOException e) {
			Logger.error("Create WebServer failed",e);
		}

		return null;
	}

	/**
	 * 构建新的 WebServer,从配置JSON中读取配置
	 *   方便从集中配置中心加载配置
	 * @param json  WebServer配置JSON
	 * @return WebServer 对象
	 */
	public static WebServer newInstance(String json) {

		try {
			if(json!=null) {
				return new WebServer(WebContext.buildConfigFromJSON(json));
			}else{
				Logger.error("Create WebServer failed: WebServerConfig object is null.");
			}
		} catch (IOException e) {
			Logger.error("Create WebServer failed",e);
		}

		return null;
	}

	/**
	 * 构建新的 WebServer,从配置文件中读取配置
	 *   方便从集中配置中心加载配置
	 * @param configFile  WebServer配置文件
	 * @return WebServer 对象
	 */
	public static WebServer newInstance(File configFile) {

		try {
			if(configFile!=null && configFile.exists()) {
				return new WebServer(WebContext.buildConfigFromFile(configFile.getCanonicalPath()));
			}else{
				Logger.error("Create WebServer failed: WebServerConfig object is null.");
			}
		} catch (IOException e) {
			Logger.error("Create WebServer failed",e);
		}

		return null;
	}

	/**
	 * 构建新的 WebServer,指定服务端口
	 * @param port  HTTP 服务的端口号
	 * @return WebServer 对象
	 */
	public static WebServer newInstance(int port) {
		WebServerConfig config = WebContext.getWebServerConfig();
		config.setPort(port);
		return newInstance(config);
	}

	/**
	 * 构建新的 WebServer,从配置文件读取配置
	 *
	 * @return WebServer 对象
	 */
	public static WebServer newInstance() {
		return newInstance(WebContext.getWebServerConfig());
	}

	/**
	 * 通用服务启动
	 */
	private void commonServe() throws IOException {

		WebContext.logo();

		//运行初始化 Class
		runWebInit(this);

		//加载过滤器,路由,模块
		WebContext.initWebServerPluginConfig(true);

		//初始化路由
		initRouter();

		//初始化模块
		initModule();

		//初始化管理路由
		InitManagerRouter();

		initSocketServer(this.config);

		//输出欢迎信息
		WebContext.welcome();

		//保存 PID
		Long pid = TEnv.getCurrentPID();
		Logger.simple("Process ID: "+ pid.toString());
		File pidFile = new File("logs/.pid");
		try {
			TFile.writeFile(pidFile, false, pid.toString().getBytes());
		} catch (IOException e) {
			Logger.error("Write pid to file: " + pidFile.getPath() + " error", e);
		}

		//保存 Token
		File tokenFile = new File("logs/.token");
		try {
			TFile.writeFile(tokenFile, false, WebContext.AUTH_TOKEN.getBytes());
		} catch (IOException e) {
			Logger.error("Write token to file: " + pidFile.getPath() + " error", e);
		}

		String serviceUrl = "http"+(config.isHttps()?"s":"")+"://"+config.getHost()+":"+config.getPort();
		Logger.simple("WebServer working on: \t" + serviceUrl);

	}

	/**
	 * 重新读取 WebConfig 的配置
	 * @param reloadInfoJson 重新读取 WebConfig 的配置信息
	 */
	public void reload(String reloadInfoJson){

		WebContext.PAUSE = true;

		//卸载模块
		unInitModule();

		WebServerConfig config = null;

		Map<String, Object> reloadInfo = (Map<String, Object>)JSON.parse(reloadInfoJson);
		if("FILE".equals(reloadInfo.get("Type"))) {
			config = WebContext.buildConfigFromFile(reloadInfo.get("Content").toString());
		}

		if("HTTP".equals(reloadInfo.get("Type"))) {
			config = WebContext.buildConfigFromRemote(reloadInfo.get("Content").toString());
		}

		if("JSON".equals(reloadInfo.get("Type"))) {
			config = WebContext.buildConfigFromJSON(reloadInfo.get("Content").toString());
		}

		this.config = config;

		//构造新的请求派发器创建
		this.httpDispatcher = new HttpDispatcher(config,sessionManager);
		this.webSocketDispatcher = new WebSocketDispatcher(config, sessionManager);

		//更新 WebServer 的 http 和 websocket 的分发
		aioServerSocket.handler(new WebServerHandler(config, httpDispatcher,webSocketDispatcher));

		//输出欢迎信息
		WebContext.welcome();

		//加载过滤器,路由,模块
		WebContext.initWebServerPluginConfig(false);

		//初始化路由
		initRouter();

		//初始化模块
		initModule();

		//初始化管理路由
		InitManagerRouter();

		WebContext.PAUSE = false;
	}

	/**
	 * 加载并运行初始化类
	 * @param webServer WebServer对象
	 */
	private void runWebInit(WebServer webServer){
		String lifeCycleClass = WebContext.getWebServerConfig().getLifeCycleClass();

		if(lifeCycleClass==null) {
			Logger.info("None WebServer lifeCycle class to load.");
			return;
		}

		if(lifeCycleClass.isEmpty()){
			Logger.info("None WebServer lifeCycle class to load.");
			return;
		}

		try {
			WebServerLifeCycle webServerLifeCycle = null;

			Class clazz = Class.forName(lifeCycleClass);
			if(TReflect.isImpByInterface(clazz, WebServerLifeCycle.class)){
				webServerLifeCycle = (WebServerLifeCycle)TReflect.newInstance(clazz);
				webServerLifeCycle.init(webServer);
			}else{
				Logger.warn("The WebServer lifeCycle class " + lifeCycleClass + " is not a class implement by " + WebServerLifeCycle.class.getName());
			}
		} catch (Exception e) {
			Logger.error("Initialize WebServer lifeCycle class error: " + e);
		}
	}

	/**
	 * 加载并运行初始化类
	 * @param webServer WebServer对象
	 */
	private void runWebDestory(WebServer webServer){
		String lifeCycleClass = WebContext.getWebServerConfig().getLifeCycleClass();

		if(lifeCycleClass==null) {
			Logger.info("None WebServer lifeCycle class to load.");
			return;
		}

		if(lifeCycleClass.isEmpty()){
			Logger.info("None WebServer lifeCycle class to load.");
			return;
		}

		try {
			WebServerLifeCycle webServerLifeCycle = null;

			Class clazz = Class.forName(lifeCycleClass);
			if(TReflect.isImpByInterface(clazz, WebServerLifeCycle.class)){
				webServerLifeCycle = (WebServerLifeCycle)TReflect.newInstance(clazz);
				webServerLifeCycle.destory(webServer);
			}else{
				Logger.warn("The WebServer lifeCycle class " + lifeCycleClass + " is not a class implement by " + WebServerLifeCycle.class.getName());
			}
		} catch (Exception e) {
			Logger.error("Initialize WebServer destory lifeCycle error: " + e);
		}
	}


	/**
	 * 是否具备管理权限
	 *      这里控制必须是 127.0.0.1的 ip 地址, 并且需要提供 authToken
	 * @param request http请求对象
	 * @return true: 具备管理权限, false: 不具备管理权限
	 */
	public static boolean hasAdminRight(HttpRequest request){
		if(!request.getRemoteAddres().equals("127.0.0.1")){
			request.getSession().close();
		}

		String authToken = request.header().get("AUTH-TOKEN");
		if(authToken!=null && authToken.equals(WebContext.AUTH_TOKEN)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 初始化服务管理相关的路由
	 */
	public void InitManagerRouter(){
		final WebServer innerWebServer = this;

		otherMethod("ADMIN", "/status", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {
				String status = "RUNNING";
				if(hasAdminRight(request)) {
					if(WebContext.PAUSE){
						status = "PAUSE";
					}
					response.write(status);
				}else{
					request.getSession().close();
				}
			}
		});

		otherMethod("ADMIN", "/shutdown", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {

				if(hasAdminRight(request)) {
					request.getSocketSession().close();
					innerWebServer.stop();
					Logger.info("WebServer is stoped");
				}else{
					request.getSession().close();
				}
			}
		});

		otherMethod("ADMIN", "/pause", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {

				if(hasAdminRight(request)) {
					WebContext.PAUSE = true;
					response.write("OK");
					Logger.info("WebServer is paused");
				}else{
					request.getSession().close();
				}
			}
		});

		otherMethod("ADMIN", "/unpause", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {

				if(hasAdminRight(request)) {
					WebContext.PAUSE = false;
					response.write("OK");
					Logger.info("WebServer is running");
				}else{
					request.getSession().close();
				}
			}
		});

		otherMethod("ADMIN", "/pid", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {

				if(hasAdminRight(request)) {
					response.write(Long.valueOf(TEnv.getCurrentPID()).toString());
				}else{
					request.getSession().close();
				}
			}
		});

		otherMethod("ADMIN", "/reload", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {

				if(hasAdminRight(request)) {
					reload(request.body().getBodyString());
					response.write("OK");
				}else{
					request.getSession().close();
				}
			}
		});

		otherMethod("ADMIN", "/authtoken", new HttpRouter() {
			@Override
			public void process(HttpRequest request, HttpResponse response) throws Exception {
				if(!request.getRemoteAddres().equals("127.0.0.1")){
					request.getSession().close();
				}

				String authToken = request.header().get("AUTH-TOKEN");
				if(authToken!=null && authToken.equals(WebContext.AUTH_TOKEN)) {
					if(!request.body().getBodyString().isEmpty()){
						//重置 AUTH_TOKEN
						WebContext.AUTH_TOKEN = request.body().getBodyString();
						response.write("OK");
					} else {
						response.write("NOTHING");
					}
				} else {
					response.write(WebContext.AUTH_TOKEN);
				}
			}
		});
	}

	/**
	 * 启动服务
	 * 		阻塞方式启动
	 *
	 * @return WebServer 对象
	 */
	public WebServer serve() {
		//接受并处理 SIGTERM 消息结束进程
		final WebServer innerWebServer = this;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				innerWebServer.stop();
			}
		});


		try {
			commonServe();
			aioServerSocket.start();
		} catch (IOException e) {
			Logger.error("Start HTTP server error",e);
		}
		return this;
	}

	/**
	 * 启动服务
	 *		非阻塞方式启动
	 * @return WebServer 对象
	 */
	public WebServer syncServe() {
		try {
			commonServe();
			aioServerSocket.syncStart();
		} catch (IOException e) {
			Logger.error("Start HTTP server error",e);
		}
		return this;
	}

	/**
	 * 获取 Http 的路由配置
	 * @return 路由配置信息
	 */
	public Map<String, Map<String, HttpRouter>> getHttpRouters(){
		return httpDispatcher.getRoutes();
	}

	/**
	 * 获取 WebSocket 的路由配置
	 * @return 路由配置信息
	 */
	public Map<String, WebSocketRouter> getWebSocketRouters(){
		return webSocketDispatcher.getRouters();
	}

	/**
	 * 是否处于服务状态
	 * @return true: 处于服务状态, false: 不处于服务状态
	 */
	public boolean isServing(){
		return aioServerSocket.isConnected();
	}

	/**
	 * 启动 WebServer 服务
	 * @param args 启动参数
	 */
	public static void main(String[] args) {
		WebServerConfig config = WebContext.getWebServerConfig();

		if(args.length>0){
			for(int i=0;i<args.length;i++){

				//服务监听地址
				if(args[i].equals("--config")){
					i++;
					config = WebContext.buildConfigFromFile(args[i]);
				}

				//服务监听地址
				if(args[i].equals("--remoteConfig")){
					i++;
					config = WebContext.buildConfigFromRemote(args[i]);
				}

				//服务监听地址
				if(args[i].equals("-h")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setHost(args[i]);
				}

				//服务端口
				if(args[i].equals("-p")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setPort(Integer.parseInt(args[i]));
				}

				//读取超时时间(s)
				if(args[i].equals("-rt")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setReadTimeout(Integer.parseInt(args[i]));
				}


				//发送超时时间(s)
				if(args[i].equals("-st")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setReadTimeout(Integer.parseInt(args[i]));
				}

				//上下文路径
				if(args[i].equals("-r")){
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
				if(args[i].equals("-mri")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setMatchRouteIgnoreCase(true);
				}

				//默认字符集,默认 UTF-8
				if(args[i].equals("--charset")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.setCharacterSet(args[i]);
				}

				//是否启用Gzip压缩,默认 true
				if(args[i].equals("--noGzip")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setGzip(false);
				}

				//是否记录access.log,默认 true
				if(args[i].equals("--noAccessLog")){
					config = config==null?WebContext.getWebServerConfig():config;
					config.setAccessLog(false);
				}

				//HTTPS 证书
				if(args[i].equals("--https.CertificateFile")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setCertificateFile(args[i]);
				}
				//证书密码
				if(args[i].equals("--https.CertificatePassword")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setCertificatePassword(args[i]);
				}
				//证书Key 密码
				if(args[i].equals("--https.KeyPassword")){
					config = config==null?WebContext.getWebServerConfig():config;
					i++;
					config.getHttps().setKeyPassword(args[i]);
				}

				//输出版本号
				if(args[i].equals("-v")){
					Logger.simple("Version:"+WebContext.getVERSION());
					return;
				}

				if(args[i].equals("--help") || args[i].equals("-?")){
					Logger.simple("Usage: java -jar voovan-framework.jar [Options]");
					Logger.simple("");
					Logger.simple("Start voovan webserver");
					Logger.simple("");
					Logger.simple("Options:");
					Logger.simple(TString.rightPad("  -h ",35,' ')+"Webserver bind host ip address");
					Logger.simple(TString.rightPad("  -p ",35,' ')+"Webserver bind port number");
					Logger.simple(TString.rightPad("  -rt ",35,' ')+"Socket read timeout");
					Logger.simple(TString.rightPad("  -st ",35,' ')+"Socket send timeout");
					Logger.simple(TString.rightPad("  -r ",35,' ')+"Context root path, contain webserver static file");
					Logger.simple(TString.rightPad("  -i ",35,' ')+"index file for client access to webserver");
					Logger.simple(TString.rightPad("  -mri ",35,' ')+"Match route ignore case");
					Logger.simple(TString.rightPad("  --config ",35,' ')+" Webserver config file");
					Logger.simple(TString.rightPad("  --remoteConfig ",35,' ')+" Remote Webserver config with a HTTP URL address");
					Logger.simple(TString.rightPad("  --charset ",35,' ')+"set default charset");
					Logger.simple(TString.rightPad("  --noGzip ",35,' ')+"Do not use gzip for client");
					Logger.simple(TString.rightPad("  --noAccessLog ",35,' ')+"Do not write access log to access.log");
					Logger.simple(TString.rightPad("  --https.CertificateFile ",35,' ')+" Certificate file for https");
					Logger.simple(TString.rightPad("  --https.CertificatePassword ",35,' ')+" Certificate passwork for https");
					Logger.simple(TString.rightPad("  --https.KeyPassword ",35,' ')+"Certificate key for https");
					Logger.simple(TString.rightPad("  --help, -?",35,' ')+"how to use this command");
					Logger.simple(TString.rightPad("  -v ",35,' ')+"Show the version information");
					Logger.simple("");

					Logger.simple("This WebServer based on VoovanFramework.");
					Logger.simple("WebSite: http://www.voovan.org");
					Logger.simple("Author: helyho");
					Logger.simple("E-mail: helyho@gmail.com");
					Logger.simple("");

					return;
				}
			}
		}


		WebServer webServer = WebServer.newInstance(config);

		webServer.serve();
	}

	/**
	 * 停止 WebServer
	 */
	public void stop(){
		try {
			System.out.println("=============================================================================================");
			System.out.println("[" + TDateTime.now() + "] Try to stop WebServer....");

			unInitModule();
			this.runWebDestory(this);

			aioServerSocket.close();
			System.out.println("[" + TDateTime.now() + "] Socket closed");

			Global.getThreadPool().shutdown();
			System.out.println("[" + TDateTime.now() + "] Thread pool is shutdown.");

			System.out.println("[" + TDateTime.now() + "] Now webServer is fully stoped.");
			TEnv.sleep(1000);
		}catch(ShutdownChannelGroupException e){
			return;
		}
	}
}
