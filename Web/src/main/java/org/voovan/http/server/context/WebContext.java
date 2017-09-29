package org.voovan.http.server.context;

import org.voovan.Global;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.WebServer;
import org.voovan.tools.*;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.log.Logger;
import org.voovan.tools.log.SingleLogger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Web上下文(配置信息读取)
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebContext {
	
	private static final String VERSION = "Voovan-WebServer/v"+ Global.getVersion();

	private static final String SESSION_NAME = "VOOVAN_SESSIONID";

	/**
	 * Web Config
	 */
	private static Map<String, Object> WEB_CONFIG = loadMapFromFile("/conf/web.json");

	/**
	 * MimeMap
	 */
	private static Map<String, Object> MIME_TYPES = loadMapFromFile("/conf/mime.json");

	/**
	 * 错误输出 Map
	 */
	private static Map<String, Object> ERROR_DEFINE = loadMapFromFile("/conf/error.json");

	/**
	 *  accessLog 的文件路径
	 */
	private static final String ACCESS_LOG_FILE_NAME = TFile.getContextPath()+ File.separator+"logs"+ File.separator+"access.log";

	private static WebServerConfig webServerConfig = buildWebServerConfig(WEB_CONFIG);

	private static final String STOP_URL = "/" + TString.generateShortUUID();

	private WebContext(){
		
	}

	public static String getStopUrl(){
		return STOP_URL;
	}
															
	/**
	 * 从 js 配置文件读取配置信息到 Map
	 * @param filePath 配置文件的路径
	 * @return Map 对象
	 */
	 public static Map<String, Object> loadMapFromFile(String filePath){
		if(TFile.fileExists(TFile.getSystemPath(filePath))) {
			String fileContent = null;
			try {
				fileContent = new String(TFile.loadFileFromContextPath(filePath),"UTF-8");
				Object configObject = JSONDecode.parse(fileContent);
				return (Map<String, Object>)configObject;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return new HashMap<String,Object>();
	}

	/**
     * 获取一个 WebServer 的配置对象
	 * @param configMap 配置对象的 Map
	 * @return WebServerConfig 对象
	 */
	public static WebServerConfig buildWebServerConfig(Map<String, Object> configMap){
		WebContext.WEB_CONFIG = configMap;
		WebContext.webServerConfig = new WebServerConfig();

		//使用反射工具自动加载配置信息
		try {
			WebContext.webServerConfig = (WebServerConfig) TReflect.getObjectFromMap(WebServerConfig.class, configMap, true);
			WebContext.webServerConfig = TObject.nullDefault(WebContext.webServerConfig, new WebServerConfig());
		} catch (ReflectiveOperationException e) {
			Logger.error(e);
		} catch (ParseException e) {
			Logger.error(e);
		}

		//如果是相对路径则转换成绝对路
		if(!WebContext.webServerConfig.getContextPath().startsWith(File.separator)){
			WebContext.webServerConfig.setContextPath(System.getProperty("user.dir")+ File.separator + WebContext.webServerConfig.getContextPath());
		}
		if(WebContext.webServerConfig.getContextPath().endsWith(File.separator)){
			WebContext.webServerConfig.setContextPath(TString.removeSuffix(WebContext.webServerConfig.getContextPath()));
		}

		return WebContext.webServerConfig;
	}

	public static void initWebServerPlugin(){
		//初始化过滤器
		webServerConfig.addFilterByList(getContextParameter("Filters",new ArrayList<Map<String,Object>>()));

		//初始路由处理器
		webServerConfig.addRouterByList(getContextParameter("Routers",new ArrayList<Map<String,Object>>()));

		//初始化模块
		webServerConfig.addModuleByList(getContextParameter("Modules",new ArrayList<Map<String,Object>>()));
		Logger.simple("=============================================================================================");
	}

	/**
	 * 获取WebServer配置对象
	 * @return WebServer配置对象
     */
	public static WebServerConfig getWebServerConfig(){
		return webServerConfig;
	}

	/**
	 * 显示欢迎信息
     */
	public static void welcome(){
		WebServerConfig config = WebContext.webServerConfig;
		Logger.simple("*********************************************************************************************");
		Logger.simple("");
		Logger.simple("   ==            ==  ==========   ==========  ==            ==  ====       ==  ==       ==	");
		Logger.simple("    ==          ==  ==        == ==        ==  ==          ==  ==  ==      ==   ==      ==	");
		Logger.simple("     ==        ==   ==        == ==        ==   ==        ==  ==    ==     ==    ==     ==	");
		Logger.simple("      ==      ==    ==        == ==        ==    ==      ==  ==      ==    ==     ==    ==   ");
		Logger.simple("       ==    ==     ==        == ==        ==     ==    ==  ============   ==      ==   ==   ");
		Logger.simple("        ==  ==      ==        == ==        ==      ==  ==  ==          ==  ==       ==  ==	");
		Logger.simple("         ====        ==========   ==========        ====  ==            == ==        == ==	");
		Logger.simple("");
		Logger.simple("*********************************************************************************************");
		Logger.simple("");
		Logger.simple("============================== [Config file parameter list] =================================");
		Logger.simple(TString.rightPad("  Timeout:",35,' ')+config.getTimeout());
		Logger.simple(TString.rightPad("  ContextPath:",35,' ')+config.getContextPath());
		Logger.simple(TString.rightPad("  CharacterSet: ",35,' ')+config.getCharacterSet());
		Logger.simple(TString.rightPad("  SessionContainer:",35,' ')+config.getSessionContainer());
		Logger.simple(TString.rightPad("  SessionTimeout:",35,' ')+config.getSessionTimeout());
		Logger.simple(TString.rightPad("  KeepAliveTimeout:",35,' ')+config.getKeepAliveTimeout());
		Logger.simple(TString.rightPad("  MatchRouteIgnoreCase:",35,' ')+config.isMatchRouteIgnoreCase());
		Logger.simple(TString.rightPad("  Gzip:",35,' ')+ config.isGzip());
		Logger.simple(TString.rightPad("  AccessLog:",35,' ')+ config.isAccessLog());

		if(config.getHotSwapInterval()>0) {
			Logger.simple(TString.rightPad("  HotSwapInterval:", 35, ' ') + config.getHotSwapInterval());
		}

		if(config.isHttps()) {
			Logger.simple(TString.rightPad("  CertificateFile:",35,' ')+config.getHttps().getCertificateFile());
			Logger.simple(TString.rightPad("  CertificatePassword:",35,' ')+config.getHttps().getCertificatePassword());
			Logger.simple(TString.rightPad("  KeyPassword:",35,' ')+config.getHttps().getKeyPassword());
		}
		Logger.simple("=============================================================================================");
		Logger.simple("  This WebServer based on VoovanFramework.");
		Logger.simple("  Version: "+WebContext.getVERSION());
		Logger.simple("  WebSite: http://www.voovan.org");
		Logger.simple("  Author: helyho");
		Logger.simple("  E-mail: helyho@gmail.com");
		Logger.simple("=============================================================================================");
	}

	/**
	 * 生成 accessLog 日志
	 * @param request   HTTP 请求对象
	 * @param response	HTTP 响应对象
	 * @return 日志信息
	 */
	private static String genAccessLog(HttpRequest request, HttpResponse response){
		StringBuilder content = new StringBuilder();
		content.append("["+ TDateTime.now()+"]");
		content.append(" "+TString.rightPad(request.getRemoteAddres(),15,' '));
		content.append(" "+TString.rightPad(request.getRemotePort()+"",5,' '));
		content.append(" "+request.protocol().getProtocol()+"/"+request.protocol().getVersion()+" "+TString.rightPad(request.protocol().getMethod(),6,' '));
		content.append(" "+response.protocol().getStatus());
		content.append(" "+response.body().getBodyBytes().length);
		content.append("\t "+request.protocol().getPath());
		content.append("\t "+TObject.nullDefault(request.header().get("User-Agent"),""));
		content.append("\t "+TObject.nullDefault(request.header().get("Referer"),""));
		content.append("\r\n");
		return content.toString();
	}
	
	/**
	 * 写入access.log
	 * @param webServerConfig WebServer 配置对象
	 * @param request HTTP 请求对象
	 * @param response HTTP 响应对象
	 */
    public static void writeAccessLog(WebServerConfig webServerConfig, HttpRequest request,HttpResponse response){
		//配置文件控制是否写入 access.log
		//监控程序的不写出 access.log
		if(webServerConfig.isAccessLog() && !request.protocol().getPath().contains("/VoovanMonitor/")) {
			SingleLogger.writeLog(ACCESS_LOG_FILE_NAME, genAccessLog(request, response));
		}
	}
	
	/**
	 * 取配置中的参数定义
	 * @param <T> 范型
	 * @param name 参数名
	 * @param defaultValue 默认值
	 * @return 参数定义
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getContextParameter(String name, T defaultValue) {
		return TObject.nullDefault((T) WEB_CONFIG.get(name),defaultValue);
	}
	
	/**
	 * 获取 mime 定义
	 * @return  MIME 定义 Map
	 */
   public static Map<String, Object> getMimeDefine() {
	   byte[] mimeDefBytes = TFile.loadResource(TEnv.classToResource(WebServer.class).replaceAll("WebServer.class","conf/mime.json"));
		Map<String, Object> mimeDefMap = new ConcurrentHashMap<String, Object>();
		try {
			Map<String, Object> systemMimeDef = (Map<String, Object>)JSONDecode.parse(new String(mimeDefBytes,"UTF-8"));
			mimeDefMap.putAll(systemMimeDef);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		mimeDefMap.putAll(MIME_TYPES);
		return mimeDefMap;
	}
	
	/**
	 * 获取错误输出定义
	 * @return  错误输出定义
	 */
    public static Map<String, Object> getErrorDefine() {
		return ERROR_DEFINE;
	}

	/**
	 * 获取版本号
	 * @return  版本号
	 */
	public final static String getVERSION() {
		return VERSION;
	}
	
	/**
	 * 获取在 Cookie 中保存 session id 的名称
	 * @return session id 的名称
	 */
	public static String getSessionName() {
		return SESSION_NAME;
	}

	/**
	 * 默认错误输出定义
	 * @return 错误输出定义
     */
    public static String getDefaultErrorPage(){
		return "RequestMethod: {{RequestMethod}} <hr/>" +
				"StatusCode: {{StatusCode}} <hr/>" +
				"RequestPath: {{RequestPath}} <hr/>" +
				"ErrorMessage: {{ErrorMessage}} <hr/>" +
				"Version: {{Version}} <hr/>" +
				"Description: <br>{{Description}} <hr/>";
	}
}
