package org.voovan.http.server;

import org.voovan.tools.*;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.log.Logger;
import org.voovan.tools.log.SingleLogger;

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
	
	private static final String VERSION = "Voovan-HTTP-Server/V1.0-alpha-2";
	
	private static final String SESSION_NAME = "VOOVAN_SESSIONID";

	/**
	 * Web Config
	 */
	private static final Map<String, Object> WEB_CONFIG = loadMapFromFile("/conf/web.json");

	/**
	 * MimeMap
	 */
	private static final Map<String, Object> MIME_TYPES = loadMapFromFile("/conf/mime.json");
	/**
	 * 错误输出 Map
	 */
	private static final Map<String, Object> ERROR_DEFINE = loadMapFromFile("/conf/error.json");

	/**
	 *  accessLog 的文件路径
	 */
	private static final String ACCESS_LOG_FILE_NAME = TEnv.getContextPath()+File.separator+"logs"+File.separator+"access.log";

	private static WebServerConfig webServerConfig = initWebServerConfig();

	private WebContext(){
		
	}
															
	/**
	 * 从 js 配置文件读取配置信息到 Map
	 * @param filePath
	 * @return
	 */
	private static Map<String, Object> loadMapFromFile(String filePath){
		if(TFile.fileExists(TEnv.getSystemPath(filePath))) {
			String fileContent = null;
			try {
				fileContent = new String(TFile.loadFileFromContextPath(filePath),"UTF-8");
				Object configObject = JSONDecode.parse(fileContent);
				return TObject.cast(configObject);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return new HashMap<String,Object>();
	}
	
	/**
	 * 从配置文件初始化 config 对象
	 * @return
	 */
	private static WebServerConfig initWebServerConfig() {
		WebServerConfig config = new WebServerConfig();

		//使用反射工具自动加载配置信息
		try {
			config = (WebServerConfig)TReflect.getObjectFromMap(WebServerConfig.class,WEB_CONFIG,true);
		} catch (ReflectiveOperationException e) {
			Logger.error(e);
		} catch (ParseException e) {
			Logger.error(e);
		}

		//如果是相对路径则转换成绝对路径
		if(!config.getContextPath().startsWith(File.separator)){
			config.setContextPath(System.getProperty("user.dir")+File.separator+config.getContextPath());
		}
		if(config.getContextPath().endsWith(File.separator)){
			config.setContextPath(TString.removeSuffix(config.getContextPath()));
		}

		//初始化过滤器
		config.addFilterByConfigs(getContextParameter("Filters",new ArrayList<Map<String,Object>>()));

		//初始路由处理器
		config.addRouterByConfigs(getContextParameter("Routers",new ArrayList<Map<String,Object>>()));

		return config;
	}

	/**
	 * 获取HttpServer配置对象
	 * @return  HttpServer配置对象
     */
	public static WebServerConfig getWebServerConfig(){
		return webServerConfig;
	}

	/**
	 * 显示欢迎信息
	 * @param config HttpServer配置对象
     */
	protected static void welcome(WebServerConfig config){
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
		Logger.simple("\tTimeout:\t\t\t\t"+config.getTimeout());
		Logger.simple("\tContextPath:\t\t\t"+config.getContextPath());
		Logger.simple("\tCharacterSet:\t\t\t"+config.getCharacterSet());
		Logger.simple("\tSessionContainer:\t\t"+config.getSessionContainer());
		Logger.simple("\tSessionTimeout:\t\t\t"+config.getSessionTimeout());
		Logger.simple("\tKeepAliveTimeout:\t\t"+config.getKeepAliveTimeout());
		Logger.simple("\tGzip:\t\t\t\t\t"+ config.isGzip());
		Logger.simple("\tAccessLog:\t\t\t\t"+ config.isAccessLog());
		if(config.getCertificateFile()!=null) {
			Logger.simple("\tCertificateFile:\t\t" + config.getCertificateFile());
			Logger.simple("\tCertificatePassword:\t" + config.getCertificatePassword());
			Logger.simple("\tKeyPassword:\t\t\t" + config.getKeyPassword());
		}
		Logger.simple("=============================================================================================");
		Logger.simple("Process ID: "+TEnv.getCurrentPID());
		Logger.simple("WebServer working on: http"+(config.getCertificateFile()!=null?"s":"")+"://"+config.getHost()+":"+config.getPort()+" ...");
	}

	/**
	 * 生成 accessLog 日志
	 * @param request   HTTP 请求对象
	 * @param response	HTTP 响应对象
	 * @return 日志信息
	 */
	private static String genAccessLog(HttpRequest request,HttpResponse response){
		StringBuilder content = new StringBuilder();
		content.append("["+TDateTime.now()+"]");
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
	 * @param request HTTP 请求对象
	 * @param response HTTP 响应对象
	 */
    protected static void writeAccessLog(HttpRequest request,HttpResponse response){
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
		return WEB_CONFIG.get(name) == null ? defaultValue : ((T) WEB_CONFIG.get(name));
	}
	
	/**
	 * 获取 mime 定义
	 * @return  MIME 定义 Map
	 */
    protected static Map<String, Object> getMimeDefine() {
		byte[] mimeDefBytes = TFile.loadResource("org/voovan/http/server/router/mime.json");
		Map<String, Object> mimeDefMap = new ConcurrentHashMap<String, Object>();
		try {
			Map<String, Object> systemMimeDef = TObject.cast(JSONDecode.parse(new String(mimeDefBytes,"UTF-8")));
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
    protected static Map<String, Object> getErrorDefine() {
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
    protected static String getDefaultErrorPage(){
		return "RequestMethod: {{RequestMethod}} <hr/>" +
				"StatusCode: {{StatusCode}} <hr/>" +
				"RequestPath: {{RequestPath}} <hr/>" +
				"ErrorMessage: {{ErrorMessage}} <hr/>" +
				"Version: {{Version}} <hr/>" +
				"Description: <br>{{Description}} <hr/>";
	}
}
