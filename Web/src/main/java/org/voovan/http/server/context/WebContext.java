package org.voovan.http.server.context;

import org.voovan.Global;
import org.voovan.http.client.HttpClient;
import org.voovan.http.message.Response;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpResponse;
import org.voovan.http.server.WebServer;
import org.voovan.tools.*;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.log.Logger;
import org.voovan.tools.log.SingleLogger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
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

	/**
	 * 管理令牌
	 */

	public static String AUTH_TOKEN = TString.generateShortUUID();

	/**
	 * 暂停标记
	 */
	public static boolean PAUSE = false;

	private static final String FRAMEWORK_NAME = "Voovan";

	private static final String SESSION_NAME = "VOOVAN_SESSIONID";

	/**
	 * Web Config
	 */
	private static Map<String, Object> WEB_CONFIG = new HashMap<String, Object>();

	/**
	 * MimeMap
	 */
	private static Map<String, Object> MIME_TYPES = new HashMap<String, Object>();

	/**
	 * 错误输出 Map
	 */
	private static Map<String, Object> ERROR_DEFINE = new HashMap<String, Object>();

	static {
		//加载默认配置
		loadWebConfig();
	}

	/**
	 *  accessLog 的文件路径
	 */
	private static final String ACCESS_LOG_FILE_NAME = TFile.getContextPath()+ File.separator+"logs"+ File.separator+"access.log";

	private static WebServerConfig webServerConfig = buildConfigFromMap(WEB_CONFIG);

	private WebContext(){

	}

	/**
	 * 是否开启请求缓存机制
	 * @return true 开启
	 */
	public static boolean isCache(){
		if(webServerConfig != null){
			return webServerConfig.isCache();
		} else {
			return false;
		}
	}

	/**
	 * 从 js 配置文件读取配置信息到 Map
	 * @param configFile 配置文件的路径
	 * @return Map 对象
	 */
	private static Map<String, Object> loadJsonFromFile(File configFile){
		if(configFile.exists()) {
			String fileContent = null;
			try {
				fileContent = new String(TFile.loadFile(configFile),"UTF-8");
				Object configObject = loadJsonFromJSON(fileContent);
				return (Map<String, Object>)configObject;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return new HashMap<String,Object>();
	}

	/**
	 * 读取配置文件
	 */
	public static void loadWebConfig(){
		synchronized (WEB_CONFIG) {
			WEB_CONFIG = loadJsonFromFile(new File(TFile.getSystemPath("/conf/web.json")));
		}

		synchronized (MIME_TYPES) {
			MIME_TYPES = loadJsonFromFile(new File(TFile.getSystemPath("/conf/mime.json")));
		}

		synchronized (ERROR_DEFINE) {
			ERROR_DEFINE = loadJsonFromFile(new File(TFile.getSystemPath("/conf/error.json")));
		}
	}

	/**
	 * 从 js 配置字符串读取配置信息到 Map
	 * @param json 配置字符串
	 * @return Map 对象
	 */
	private static Map<String, Object> loadJsonFromJSON(String json){
		if(json != null) {
			Object configObject = JSONDecode.parse(json);
			return (Map<String, Object>)configObject;
		}
		return new HashMap<String,Object>();
	}

	/**
	 * 获取一个 WebServer 的配置对象
	 * @param configMap 配置对象的 Map
	 * @return WebServerConfig 对象
	 */
	public static WebServerConfig buildConfigFromMap(Map<String, Object> configMap){

		if(configMap.size() > 0 ) {
			WebContext.WEB_CONFIG = configMap;
		}

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

	/**
	 * 从一个配置文件, 获取一个 WebServer 的配置对象
	 * @param configFilePath 配置文件
	 * @return WebServerConfig 对象
	 */
	public static WebServerConfig buildConfigFromFile(String configFilePath) {
		String configFileFullPath = TFile.getContextPath()+ File.separator + configFilePath;
		File configFile = new File(configFileFullPath);
		if(configFile.exists()) {
			return buildConfigFromMap(loadJsonFromFile(configFile));
		}else{
			Logger.warn("Use the config file: " + configFilePath + " is not exists, now use default config.");
			return null;
		}
	}

	/**
	 * 从一个配置字符串, 获取一个 WebServer 的配置对象
	 * @param json 配置文件
	 * @return WebServerConfig 对象
	 */
	public static WebServerConfig buildConfigFromJSON(String json){
		return buildConfigFromMap(loadJsonFromJSON(json));
	}

	/**
	 * 从一个配置字符串, 获取一个 WebServer 的配置对象
	 * @param httpUrl 配置文件
	 * @return WebServerConfig 对象
	 */
	public static WebServerConfig buildConfigFromRemote(String httpUrl){
		HttpClient httpClient = null;
		try {
			URL url = new URL(httpUrl);
			httpClient = new HttpClient(url.getProtocol()+"://"+url.getHost()+":"+url.getPort());
			Response response = httpClient.send(url.getPath());
			if(response.protocol().getStatus() == 200) {
				return WebContext.buildConfigFromJSON(response.body().getBodyString());
			}else{
				throw new IOException("Get the config url: \" + args[i] + \" error");
			}
		} catch (Exception e) {
			Logger.error("Use the config url: " + httpUrl + " error", e);
		} finally {
			if(httpClient!=null){
				httpClient.close();
			}
		}

		return null;
	}

	public static void initWebServerPluginConfig(boolean isInit){
		//初始化过滤器
		if(!isInit) {
			webServerConfig.getFilterConfigs().clear();
		}
		webServerConfig.addFilterByList(getContextParameter("Filters",new ArrayList<Map<String,Object>>()));

		//初始路由处理器
		if(!isInit) {
			webServerConfig.getRouterConfigs().clear();
		}
		webServerConfig.addRouterByList(getContextParameter("Routers",new ArrayList<Map<String,Object>>()));

		//初始化模块
		if(!isInit) {
			webServerConfig.getModuleonfigs().clear();
		}
		webServerConfig.addModuleByList(getContextParameter("Modules",new ArrayList<Map<String,Object>>()));
		Logger.simple("==================================================================================================================================================");
	}

	/**
	 * 获取WebServer配置对象
	 * @return WebServer配置对象
	 */
	public static WebServerConfig getWebServerConfig(){
		return webServerConfig;
	}


	public static void logo(){
		System.out.println("==================================================================================================================================================");
		System.out.println("");
		System.out.println("       *************************************  ****        ****  ************   ***********   ****        ****     ****       ***********     ");
		System.out.println("       *************************************   *****      **** **************  *************  *****      ****     ******    **************   ");
		System.out.println("       ***************      ****************   *****      **** **************  ************** *****      ****    ********   ***************  ");
		System.out.println("       **********                ***********   *****     ****  **************  ************** *****     ****    *********   ***************  ");
		System.out.println("       *********                  **********   *****     **** *************** *************** *****     ****   **********   ***************  ");
		System.out.println("       *******     ***********      ********   *****     **** *************** ***************  ****     ****  ***********   ***************  ");
		System.out.println("       ******    ***************    ********    ****     **** *************** ***************  *****    ****  ***********   ***************  ");
		System.out.println("       *****    *****************    *******    *****    ****  *****     ****  ****      ****  *****    ****  ****** ****    *****     ****  ");
		System.out.println("       *****    *******************   ******     ****   ****   ****      ****  *****     ****  *****    ****  ***********    ****      ****  ");
		System.out.println("       **********        ********     ******     ****   ****   ****      ****  ****      ****   ****   ****   *****  ****    ****      ****  ");
		System.out.println("       *********       ********            *     ****  ****    ****      ****  ****      ****   ****   ****   *****  ****    ****     ****   ");
		System.out.println("       *             ***********************     ****  ****    ****      ****  ****     ****    ****  ****    *****  ****    ****     ****   ");
		System.out.println("       ****    *******************   *******     ****  ****    ****     ****   ****     ****    ****  ****    *****  ****   ****      ****   ");
		System.out.println("       *************************     *******     ****  ****    ****     ****   ****     ****    ****  ****    ****   ****   ****      ****   ");
		System.out.println("       *****     **************     ********     ***** ****   ****      ****  ****      ****    **** ****    *****   ****   ****      ****   ");
		System.out.println("       ******     ************      ********     *********    ****      ****  ****      ****    **** ****    ************   ****      ****   ");
		System.out.println("       *******      ********       *********     **** ****    *************** **************    **** ****    ************   ****     *****   ");
		System.out.println("       ********                  ***********     *********    **************  **************    *********    ************   ****     *****   ");
		System.out.println("       **********              *************     ********     **************  **************    *********    ************   ****     *****   ");
		System.out.println("       **************      *****************     ********     *************   *************     ********     *****   ****   ****     *****   ");
		System.out.println("       *************************************      *******     *************   *************      ******     *****     **** ****      *****   ");
		System.out.println("       *************************************      *******      ***********     ***********        ****     *****      **** ****      *****   ");
		System.out.println("");
		System.out.println("==================================================================================================================================================");
	}

	/**
	 * 显示欢迎信息
	 */
	public static void welcome(){
		WebServerConfig config = WebContext.webServerConfig;
		System.out.println("========================================================= [Config file parameter list] ===========================================================");
		System.out.println(TString.rightPad("  ReadTimeout:",35,' ')+config.getReadTimeout());
		System.out.println(TString.rightPad("  SendTimeout:",35,' ')+config.getSendTimeout());
		System.out.println(TString.rightPad("  ContextPath:",35,' ')+config.getContextPath());
		System.out.println(TString.rightPad("  CharacterSet: ",35,' ')+config.getCharacterSet());
		System.out.println(TString.rightPad("  SessionContainer:",35,' ')+config.getSessionContainer());
		System.out.println(TString.rightPad("  SessionTimeout:",35,' ')+config.getSessionTimeout());
		System.out.println(TString.rightPad("  KeepAliveTimeout:",35,' ')+config.getKeepAliveTimeout());
		System.out.println(TString.rightPad("  MatchRouteIgnoreCase:",35,' ')+config.isMatchRouteIgnoreCase());
		System.out.println(TString.rightPad("  Gzip:",35,' ')+ config.isGzip());
		System.out.println(TString.rightPad("  GzipMinSize:",35,' ')+ config.getGzipMinSize());
		System.out.println(TString.rightPad("  GzipMimeType:",35,' ')+ config.getGzipMimeType());
		System.out.println(TString.rightPad("  AccessLog:",35,' ')+ config.isAccessLog());
		System.out.println(TString.rightPad("  Cache:",35,' ')+ config.isCache());
		System.out.println(TString.rightPad("  PauseURL:",35,' ')+ config.getPauseURL());
		System.out.println(TString.rightPad("  MaxRequestSize:",35,' ')+ config.getMaxRequestSize());

		if(config.getHotSwapInterval()>0) {
			System.out.println(TString.rightPad("  HotSwapInterval:", 35, ' ') + config.getHotSwapInterval());
		}

		if(config.getScanAopPackage()!=null) {
			System.out.println(TString.rightPad("  ScanAopPackage:", 35, ' ') + config.getScanAopPackage());
		}

		if(config.isHttps()) {
			System.out.println(TString.rightPad("  CertificateFile:",35,' ')+config.getHttps().getCertificateFile());
			System.out.println(TString.rightPad("  CertificatePassword:",35,' ')+config.getHttps().getCertificatePassword());
			System.out.println(TString.rightPad("  KeyPassword:",35,' ')+config.getHttps().getKeyPassword());
		}

		System.out.println(TString.rightPad("  AuthToken:",35,' ')+ AUTH_TOKEN);

		System.out.println("==================================================================================================================================================");
		System.out.println("  This WebServer based on VoovanFramework.");
		System.out.println("  Version: "+WebContext.getVERSION());
		System.out.println("  WebSite: http://www.voovan.org");
		System.out.println("  Author: helyho");
		System.out.println("  E-mail: helyho@gmail.com");
		System.out.println("==================================================================================================================================================");
	}


	/**
	 * 获取 AuthToken
	 */
	public static void getAuthToken(){
		//保存 Token
		File tokenFile = new File("logs/.token");

		if(tokenFile.exists()){
			WebContext.AUTH_TOKEN = new String(TFile.loadFile(tokenFile));
		} else {
			try {
				TFile.writeFile(tokenFile, false, WebContext.AUTH_TOKEN.getBytes());
			} catch (IOException e) {
				Logger.error("Write token to file: " + tokenFile.getPath() + " error", e);
			}
		}
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
		content.append(" "+response.body().size());
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
		return "v" + Global.getVersion();
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
