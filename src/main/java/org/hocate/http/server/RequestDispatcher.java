package org.hocate.http.server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.hocate.http.message.packet.Cookie;
import org.hocate.http.server.Exception.RouterNotFound;
import org.hocate.http.server.router.MimeFileRouter;
import org.hocate.tools.TFile;
import org.hocate.tools.TObject;
import org.hocate.tools.TString;

/**
 * 
 * 根据 Request 请求分派到处理路由
 * 
 * 
 * GET 请求获取Request-URI所标识的资源<br/>
 * POST 在Request-URI所标识的资源后附加新的数据<br/>
 * HEAD 请求获取由Request-URI所标识的资源的响应消息报头<br/>
 * PUT 请求服务器存储一个资源，并用Request-URI作为其标识<br/>
 * DELETE 请求服务器删除Request-URI所标识的资源<br/>
 * TRACE 请求服务器回送收到的请求信息，主要用于测试或诊断<br/>
 * CONNECT 保留将来使用<br/>
 * OPTIONS 请求查询服务器的性能，或者查询与资源相关的选项和需求<br/>
 * 
 * @author helyho
 *
 */
public class RequestDispatcher {
	/**
	 * [MainKey] = HTTP method ,[Value Key] = Route path, [Value value] = RouteBuiz对象
	 */
	private Map<String, Map<String, Router>>	routes;
	private SessionManager sessionManager;
	private WebConfig config;
	
	/**
	 * 构造函数
	 * 
	 * @param rootDir
	 *            根目录
	 */
	public RequestDispatcher(WebConfig config) {
		routes = new HashMap<String, Map<String, Router>>();
		this.config = config;
		
		//构造 SessionManage
		sessionManager = SessionManager.newInstance(config);

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
		addRouteRuler("GET", MimeTools.getMimeTypeRegex(), new MimeFileRouter(config.getContextPath()));
	}

	/**
	 * 增加新的路由方法,例如:HTTP 方法 GET、POST 等等
	 * 
	 * @param method
	 */
	protected void addRouteMethod(String method) {
		if (!routes.containsKey(method)) {
			routes.put(method, new HashMap<String, Router>());
		}
	}

	/**
	 * 增加一个路由规则
	 * 
	 * @param Method
	 * @param routeRegexPath
	 * @param routeBuiz
	 */
	public void addRouteRuler(String Method, String routeRegexPath, Router routeBuiz) {
		if (routes.keySet().contains(Method)) {
			routes.get(Method).put(routeRegexPath, routeBuiz);
		}
	}

	/**
	 * 路由处理函数
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	public void Process(HttpRequest request, HttpResponse response){
		String requestPath = request.protocol().getPath();
		String requestMethod = request.protocol().getMethod();
		
		boolean isMatched = false;
		Map<String, Router> routeInfos = routes.get(requestMethod);
		for (String routePath : routeInfos.keySet()) {
			//路由匹配
			isMatched = matchPath(requestPath,routePath);
			if (isMatched) {
				//获取路由处理对象
				Router routeBuiz = routeInfos.get(routePath);
				try {
					//获取路径变量
					Map<String, String> pathVariables = fetchPathVariables(requestPath,routePath);
					request.getParameters().putAll(pathVariables);
					//Session预处理
					diposeSession(request,response);
					//处理路由请求
					routeBuiz.Process(request, response);
				} catch (Exception e) {
					ExceptionMessage(request, response, e);
				}
				break;
			}
		}
		
		//没有找寻到匹配的路由处理器
		if(!isMatched){
			ExceptionMessage(request, response,  new RouterNotFound("Not avaliable router!"));
		}
	}
	
	/**
	 * 路径匹配
	 * @param requestPath
	 * @param routeRegexPath
	 * @return
	 */
	public boolean matchPath(String requestPath, String routeRegexPath){
		//转换成可以配置的正则,主要是处理:后的参数表达式
		//把/home/:name转换成/home/[^/?]+来匹配
		String regexPath = routeRegexPath.replaceAll(":[^/$]+", "[^/?]+");
		if (TString.searchByRegex(requestPath, regexPath).length>0){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 获取路径变量,形如:/test/:name 的路径匹配的请求路径/test/var1后得到{name:var1}
	 * @param requestPath
	 * @param routePath
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public Map<String, String> fetchPathVariables(String requestPath,String routePath) throws UnsupportedEncodingException{
		Map<String, String> resultMap = new HashMap<String, String>();
		String[] pathPieces = requestPath.split("/");
		String[] routePathPieces = routePath.substring(1, routePath.length()-1).split("/");
		for(int i=0;i<routePathPieces.length;i++){
			String routePathPiece = routePathPieces[i];
			if(routePathPiece.startsWith(":")){
				String name = routePathPiece.substring(1,routePathPiece.length());
				String value = URLDecoder.decode(pathPieces[i], "UTF-8");
				resultMap.put(name,value);
			}
		}
		return resultMap;
	}
	
	/**
	 * 处理 Session
	 * @param request
	 * @param response
	 */
	public void diposeSession(HttpRequest request, HttpResponse response){
		
		//获取请求的 Cookie中的session标识
		Cookie sessionCookie = request.getCookie(WebContext.getSessionName());
		
		//如果 session 不存在,创建新的 session
		//1.保存 session id 的 cookie 不存在
		//2.从 cookie 中取 session id 为空
		//3.从 session 管理器中取 session 对象为空
		if(sessionCookie==null 
				|| TString.isNullOrEmpty(sessionCookie.getValue()) 
				|| sessionManager.getSession(sessionCookie.getValue())==null){
			//构建 session
			HttpSession session = sessionManager.newHttpSession();
			request.setSession(session);
			sessionManager.addSession(session);
			
			//取 session 超时时间
			int sessionTimeout = config.getSessionTimeout();
			//创建 Cookie
			Cookie cookie = Cookie.newInstance(request, WebContext.getSessionName(), 
					session.getId(),sessionTimeout*60);
			response.cookies().add(cookie);
		}else{
			//通过 Cookie 中的 session 标识获取 Session
			HttpSession session = sessionManager.getSession(sessionCookie.getValue());
			request.setSession(session);
		}
	}

	/**
	 * 异常消息处理
	 * 
	 * @param request
	 * @param response
	 * @param e
	 */
	public void ExceptionMessage(HttpRequest request, HttpResponse response, Exception e) {
		//输出异常
		e.printStackTrace();
		
		Map<String, Object> errorDefine = WebContext.getErrorDefine();
		String requestMethod = request.protocol().getMethod();
		String requestPath = request.protocol().getPath();
		response.header().put("Content-Type", "text/html");

		//信息准备
		String className = e.getClass().getName();
		String errorMessage = e.toString();
		String stackInfo = "";
		for (StackTraceElement stackTraceElement : e.getStackTrace()) {
			stackInfo += stackTraceElement.toString();
			stackInfo += "<br/>\r\n";
		}

		//初始 error 定义,如果下面匹配到了定义的错误则定义的会被覆盖
		Map<String, Object> error = new HashMap<String, Object>();
		error.put("StatusCode", 500);
		error.put("Page", "Error.html");
		error.put("Description", stackInfo);
		
		//读取 error 定义,如果有可用消息则会覆盖上面的初始内容
		if (errorDefine.containsKey(className)) {
			error.putAll(TObject.cast(errorDefine.get(className)));
			response.protocol().setStatus(TObject.cast(error.get("StatusCode")));
		} else if (errorDefine.get("Other") != null) {
			error.putAll(TObject.cast(errorDefine.get("Other")));
			response.protocol().setStatus(TObject.cast(error.get("StatusCode")));
		}
		
		//消息拼装
		String errorPageContent = new String(TFile.loadFileFromContextPath("/Config/ErrorPage/" + error.get("Page")));
		if(errorPageContent!=null){
			errorPageContent = TString.tokenReplace(errorPageContent, "StatusCode", error.get("StatusCode").toString());
			errorPageContent = TString.tokenReplace(errorPageContent, "RequestMethod", requestMethod);
			errorPageContent = TString.tokenReplace(errorPageContent, "RequestPath", requestPath);
			errorPageContent = TString.tokenReplace(errorPageContent, "ErrorMessage", errorMessage);
			errorPageContent = TString.tokenReplace(errorPageContent, "Description", error.get("Description").toString());
			errorPageContent = TString.tokenReplace(errorPageContent, "Version", WebContext.getVersion());
			response.clear();
			response.write(errorPageContent);
		}
	}
}
