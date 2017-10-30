package org.voovan.http.server;

import org.voovan.http.message.packet.Cookie;
import org.voovan.http.server.context.HttpFilterConfig;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.exception.ResourceNotFound;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.server.router.MimeFileRouter;
import org.voovan.tools.log.Logger;
import org.voovan.tools.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

/**
 *
 * 根据 Request 请求分派到处理路由
 *
 *
 * GET 请求获取Request-URI所标识的资源
 * POST 在Request-URI所标识的资源后附加新的数据
 * HEAD 请求获取由Request-URI所标识的资源的响应消息报头
 * PUT 请求服务器存储一个资源，并用Request-URI作为其标识
 * DELETE 请求服务器删除Request-URI所标识的资源
 * TRACE 请求服务器回送收到的请求信息，主要用于测试或诊断
 * CONNECT 保留将来使用
 * OPTIONS 请求查询服务器的性能，或者查询与资源相关的选项和需求
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpDispatcher {

	private static Map<String, String> REGEXED_ROUTER = new ConcurrentHashMap<String, String>();
	private static Map<String, List<Object>> ROUTER_CACHE = new ConcurrentHashMap<String, List<Object>>();

	/**
	 * [MainKey] = HTTP method ,[Value] = { [Value Key] = Route path, [Value value] = RouteBuiz对象 }
	 */
	private Map<String, Map<String, HttpRouter>> methodRouters;
	private WebServerConfig webConfig;
	private SessionManager sessionManager;
	private MimeFileRouter mimeFileRouter;
	private String[] indexFiles;

	/**
	 * 构造函数
	 *
	 * @param webConfig    Web 服务配置对象
	 * @param sessionManager Session 管理器
	 */
	public HttpDispatcher(WebServerConfig webConfig, SessionManager sessionManager) {
		methodRouters = new LinkedHashMap<String, Map<String, HttpRouter>>();
		this.webConfig = webConfig;
		this.sessionManager = sessionManager;

		//拆分首页索引文件的名称
		indexFiles = webConfig.getIndexFiles();

		// 初始化所有的 HTTP 请求方法
		this.addRouteMethod("GET");
		this.addRouteMethod("POST");
		this.addRouteMethod("HEAD");
		this.addRouteMethod("PUT");
		this.addRouteMethod("DELETE");
		this.addRouteMethod("TRACE");
		this.addRouteMethod("CONNECT");
		this.addRouteMethod("OPTIONS");

		// Mime静态文件默认请求处理
		mimeFileRouter = new MimeFileRouter(webConfig.getContextPath());
		addRouteHandler("GET", MimeTools.getMimeTypeRegex(), mimeFileRouter);
	}

	/**
	 * 获取 Http 的路由配置
	 * @return 路由配置信息
	 */
	public Map<String, Map<String, HttpRouter>> getRoutes(){
		return methodRouters;
	}

	/**
	 * 增加新的路由方法,例如:HTTP 方法 GET、POST 等等
	 *
	 * @param method HTTP 请求方法
	 */
	protected void addRouteMethod(String method) {
		if (!methodRouters.containsKey(method)) {
			Map<String,HttpRouter> routers = new TreeMap<String, HttpRouter>(new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if(o1.length() > o2.length() && !o1.equals(o2)){
						return -1;
					} else if(o1.length() < o2.length() &&!o1.equals(o2)){
						return 1;
					} else if(o1.equals(o2)){
						return 0;
					} else{
						return 1;
					}
				}
			});
			methodRouters.put(method, routers);
		}
	}


	/**
	 * 修复路由为规范的可注册的路由
	 * @param routePath  路由
	 * @return 规范的可注册的路由
	 */
	public static String fixRoutePath(String routePath){
		//对于结束符为"/"的路径,清理这个符号,以便更好完成的匹配
		if(routePath.endsWith("/")){
			routePath = TString.removePrefix(routePath);
		}

		//对于起始符不为"/"的路径,补充这个符号,以便更好完成的匹配
		if(!routePath.startsWith("/")){
			routePath = "/" + routePath;
		}

		//把连续的////替换成/
		return TString.fastReplaceAll(routePath, "\\/{2,9}", "/");
	}

	/**
	 * 增加一个路由规则
	 *
	 * @param method          Http 请求方法
	 * @param routeRegexPath  路径匹配正则
	 * @param router         请求处理句柄
	 */
	public void addRouteHandler(String method, String routeRegexPath, HttpRouter router) {
		if (methodRouters.keySet().contains(method)) {
			methodRouters.get(method).put(fixRoutePath(routeRegexPath), router);
		}
	}

	/**
	 * Http 请求响应处理函数,入口函数
	 *
	 * @param request    HTTP 请求
	 * @param response   HTTP 响应
	 */
	public void process(HttpRequest request, HttpResponse response){
		Chain<HttpFilterConfig> filterConfigs = webConfig.getFilterConfigs().clone();

		Object filterResult = null;

		request.setSessionManager(sessionManager);




		//正向过滤器处理,请求有可能被 Redirect 所以过滤器执行放在开始
		filterResult = disposeFilter(filterConfigs,request,response);

		//如果 response 在过滤器中修改过,则不执行路由处理
		if(response.body().size()==0) {
			//调用处理路由函数
			disposeRoute(request, response);
		}

		HttpSession session = request.getSession();
		session.save();

		//向 HttpResponse 中放置 Session 的 Cookie
		if(!session.attribute().isEmpty()) {

			if(request.getCookie(WebContext.getSessionName())==null) {

				//创建 Cookie
				Cookie cookie = Cookie.newInstance(request, "/", WebContext.getSessionName(),
						session.getId(), webConfig.getSessionTimeout() * 60);

				//响应增加Session 对应的 Cookie
				response.cookies().add(cookie);
			}
		}

		//反向过滤器处理
		filterResult = disposeInvertedFilter(filterConfigs,request,response);

		//输出访问日志
		WebContext.writeAccessLog(webConfig, request, response);
	}

	/**
	 * 获取路由处理对象和注册路由
	 * @param request 请求对象
	 * @return 路由信息对象 [ 匹配到的已注册路由, HttpRouter对象 ]
	 */
	public List<Object> findRouter(HttpRequest request){
		String requestPath 		= request.protocol().getPath();
		String requestMethod 	= request.protocol().getMethod();
		String routerMark = requestPath+requestMethod;

		List<Object> routerInfo = ROUTER_CACHE.get(routerMark);

		if(routerInfo==null) {
			Map<String, HttpRouter> routers = methodRouters.get(requestMethod);
			for (Map.Entry<String, HttpRouter> routeEntry : routers.entrySet()) {
				String routePath = routeEntry.getKey();
				//寻找匹配的路由对象
				if (matchPath(requestPath, routePath, webConfig.isMatchRouteIgnoreCase())) {
					//[ 匹配到的已注册路由, HttpRouter对象 ]
					routerInfo = TObject.asList(routePath, routeEntry.getValue());
					ROUTER_CACHE.put(routerMark, routerInfo);
					return routerInfo;
				}
			}
		}

		return routerInfo;
	}

	/**
	 * Http 路由处理函数
	 * @param request    Http请求对象
	 */
	public void disposeRoute(HttpRequest request, HttpResponse response){
		String requestPath = request.protocol().getPath();

		//[ 匹配到的已注册路由, HttpRouter对象 ]
		List<Object> routerInfo = findRouter(request);

		if (routerInfo!=null) {
			try {
				String routePath = (String)routerInfo.get(0);
				HttpRouter router = (HttpRouter)routerInfo.get(1);

				//获取路径变量
				Map<String, String> pathVariables = fetchPathVariables(requestPath, routePath);
				if(pathVariables!=null) {
					request.getParameters().putAll(pathVariables);
				}

				//处理路由请求
				router.process(request, response);

			} catch (Exception e) {
				exceptionMessage(request, response, e);
			}

		} else {
			//如果匹配失败,尝试用定义首页索引文件的名称
			if(!tryIndex(request,response)) {
				exceptionMessage(request, response, new RouterNotFound("Not avaliable router!"));
			}
		}
	}

	/**
	 * 尝试用定义首页索引文件的名称
	 * @param request   Http 请求对象
	 * @param response  Http 响应对象
	 * @return 成功匹配到定义首页索引文件的名返回 true,否则返回 false
	 */
	public boolean tryIndex(HttpRequest request,HttpResponse response){
		for (String indexFile : indexFiles) {
			String requestPath 	= request.protocol().getPath();
			String filePath = webConfig.getContextPath() + requestPath.replace("/",File.separator) + (requestPath.endsWith("/") ? "" : File.separator) + indexFile;
			if(TFile.fileExists(filePath)){
				try {
					String newRequestPath = requestPath + (requestPath.endsWith("/") ? "" : "/") + indexFile;
					request.protocol().setPath(newRequestPath);
					mimeFileRouter.process(request,response);
				} catch (Exception e) {
					exceptionMessage(request, response, e);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * 将路径转换成正则表达式形式的路径
	 * @param routePath   匹配路径参数
	 * @return  转换后的正则匹配路径
	 */
	public static String routePath2RegexPath(String routePath){
		if(!REGEXED_ROUTER.containsKey(routePath)) {
			String routeRegexPath = TString.fastReplaceAll(routePath, "\\*", ".*?");
			routeRegexPath = TString.fastReplaceAll(routeRegexPath, "/", "\\/");
			routeRegexPath = TString.fastReplaceAll(routeRegexPath, ":[^:?/_-]*", "[^:?/_-]*");
			routeRegexPath = "^/?" + routeRegexPath + "/?$";
			REGEXED_ROUTER.put(routePath, routeRegexPath);
			return routeRegexPath;
		} else {
			return REGEXED_ROUTER.get(routePath);
		}
	}

	/**
	 * 路径匹配
	 * @param requestPath    请求路径
	 * @param routePath      正则匹配路径
	 * @param matchRouteIgnoreCase 路劲匹配是否忽略大消息
	 * @return  是否匹配成功
	 */
	public static boolean matchPath(String requestPath, String routePath,boolean matchRouteIgnoreCase){
		//转换成可以配置的正则,主要是处理:后的参数表达式
		//把/home/:name转换成^[/]?/home/[/]?+来匹配
		String routeRegexPath = routePath2RegexPath(routePath);
		//匹配路由不区分大小写
		if(matchRouteIgnoreCase){
			requestPath = requestPath.toLowerCase();
			routeRegexPath = routeRegexPath.toLowerCase();
		}
		if(TString.regexMatch(requestPath, routeRegexPath ) > 0 ){
			return true;
		}else {
			return false;
		}
	}

	/**
	 * 获取路径变量,形如/:test/:name 的路径匹配的请求路径/test/var1后得到{name:var1}
	 * @param requestPath   请求路径
	 * @param routePath     正则匹配路径
	 * @return     路径抽取参数 Map
	 */
	public static Map<String, String> fetchPathVariables(String requestPath,String routePath) {
		//修正请求和匹配路由检查是否存在路径请求参数
		String compareRoutePath = routePath.endsWith("*") ? TString.removeSuffix(routePath) : routePath;
		compareRoutePath = compareRoutePath.endsWith("/") ? TString.removeSuffix(compareRoutePath) : compareRoutePath;
		String compareRequestPath = requestPath.endsWith("/") ? TString.removeSuffix(requestPath) : requestPath;

		//判断是否存在路径请求参数
		if(compareRequestPath.equals(compareRoutePath)){
			return null;
		} else {
			Map<String, String> resultMap = new LinkedHashMap<String, String>();
			String routePathMathchRegex = routePath;


			try {
				//抽取路径中的变量名
				String[] names = TString.searchByRegex(routePath, ":[^:?/_-]*");
				if (names.length > 0) {
					for (int i = 0; i < names.length; i++) {
						names[i] = TString.removePrefix(names[i]);
						String name = names[i];
						//拼装通过命名抽取数据的正则表达式
						routePathMathchRegex = routePathMathchRegex.replace(":" + name, "(?<" + name + ">.*)");
					}

					//运行正则
					Matcher matcher = TString.doRegex(requestPath, routePathMathchRegex);

					for (String name : names) {
						resultMap.put(name, URLDecoder.decode(matcher.group(name), "UTF-8"));
					}
				}
			} catch (UnsupportedEncodingException e) {
				Logger.error("RoutePath URLDecoder.decode failed by charset: UTF-8", e);
			}

			return resultMap;
		}
	}

	/**
	 * //正向处理过滤器
	 * @param filterConfigs   HTTP过滤器配置对象
	 * @param request		  请求对象
	 * @param response		  响应对象
	 * @return 过滤器最后的结果
	 */
	public Object disposeFilter(Chain<HttpFilterConfig> filterConfigs, HttpRequest request, HttpResponse response) {
		filterConfigs.rewind();
		Object filterResult = null;
		while(filterConfigs.hasNext()){
			HttpFilterConfig filterConfig = filterConfigs.next();
			HttpFilter httpFilter = filterConfig.getHttpFilterInstance();
			if(httpFilter!=null) {
				filterResult = httpFilter.onRequest(filterConfig, request, response, filterResult);
			}
		}

		return filterResult;
	}

	/**
	 * 反向处理过滤器
	 * @param filterConfigs   HTTP过滤器配置对象
	 * @param request		  请求对象
	 * @param response		  响应对象
	 * @return 过滤器最后的结果
	 */
	public Object disposeInvertedFilter(Chain<HttpFilterConfig> filterConfigs, HttpRequest request, HttpResponse response) {
		filterConfigs.rewind();
		Object filterResult = null;
		while(filterConfigs.hasPrevious()){
			HttpFilterConfig filterConfig = filterConfigs.previous();
			HttpFilter httpFilter = filterConfig.getHttpFilterInstance();
			if(httpFilter!=null) {
				filterResult = httpFilter.onResponse(filterConfig, request, response, filterResult);
			}
		}

		return filterResult;
	}

	/**
	 * 异常消息处理
	 *
	 * @param request  请求对象
	 * @param response 响应对象
	 * @param e  异常对象
	 */
	public void exceptionMessage(HttpRequest request, HttpResponse response, Exception e) {

		//获取配置文件异常定义
		Map<String, Object> errorDefine = WebContext.getErrorDefine();

		//信息准备
		String requestMethod = request.protocol().getMethod();
		String requestPath = request.protocol().getPath();
		String className = e.getClass().getName();
		String errorMessage = e.toString().replace(TFile.getLineSeparator(), "<br/>");

		String stackInfo = "";

		if(!errorDefine.containsKey(className)) {
			Throwable throwable = e;
			do {
				stackInfo = stackInfo + "\n\n" + throwable.toString() + "\n" + TEnv.getStackElementsMessage(throwable.getStackTrace());
				throwable = throwable.getCause();

				if (throwable == null) {
					break;
				}

				if (throwable instanceof InvocationTargetException) {
					throwable = (Exception) throwable.getCause();
				}

			} while (true);
		}


		//转换成能在 HTML 中展示的超文本字符串
		stackInfo = TString.indent(stackInfo.trim(),1).replace("\n", "<br>");
		response.header().put("Content-Type", "text/html");

		//初始 error 定义,如果下面匹配到了定义的错误则定义的会被覆盖
		Map<String, Object> error = new HashMap<String, Object>();

		//输出异常
		if( !(e instanceof ResourceNotFound || e instanceof RouterNotFound) ){
			response.protocol().setStatus(500);
			error.put("StatusCode", 500);
			Logger.error(e);
		}else{
			response.protocol().setStatus(404);
			error.put("StatusCode", 404);
		}
		error.put("Page", "Error.html");
		error.put("Description", stackInfo);

		//匹配 error 定义,如果有可用消息则会覆盖上面定义的初始内容
		if (errorDefine.containsKey(className)) {
			error.putAll((Map<String,Object>)errorDefine.get(className));
			response.protocol().setStatus((int)error.get("StatusCode"));
		} else if (errorDefine.get("Other") != null) {
			error.putAll((Map<String,Object>)errorDefine.get("Other"));
			response.protocol().setStatus((int)error.get("StatusCode"));
		}

		//消息拼装
		String errorPageContent = WebContext.getDefaultErrorPage();
		if(TFile.fileExists(TFile.getSystemPath("/conf/error-page/" + error.get("Page")))) {
			try {
				errorPageContent = new String(TFile.loadFileFromContextPath("/conf/error-page/" + error.get("Page")),"UTF-8");
			} catch (UnsupportedEncodingException e1) {
				Logger.error("This charset is unsupported",e);
			}
		}
		if(errorPageContent!=null){
			errorPageContent = TString.oneTokenReplace(errorPageContent, "StatusCode", error.get("StatusCode").toString());
			errorPageContent = TString.oneTokenReplace(errorPageContent, "RequestMethod", requestMethod);
			errorPageContent = TString.oneTokenReplace(errorPageContent, "RequestPath", requestPath);
			errorPageContent = TString.oneTokenReplace(errorPageContent, "ErrorMessage", errorMessage);
			errorPageContent = TString.oneTokenReplace(errorPageContent, "Description", error.get("Description").toString());
			errorPageContent = TString.oneTokenReplace(errorPageContent, "Version", WebContext.getVERSION());
			errorPageContent = TString.oneTokenReplace(errorPageContent, "DateTime", TDateTime.now());
			response.clear();
			response.write(errorPageContent);
		}
	}
}
