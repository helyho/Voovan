package org.voovan.http.server;

import org.voovan.tools.*;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.log.Logger;
import org.voovan.tools.log.SingleLogger;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Web上下文(配置信息读取)
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebContext {
	
	private static final String VERSION = "Voovan-HTTP-Server/V0.97";
	
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
	
	private WebContext(){
		
	}
															
	/**
	 * 从 js 配置文件读取配置信息到 Map
	 * @param filePath
	 * @return
	 */
	private static Map<String, Object> loadMapFromFile(String filePath){
		String fileContent = new String(TFile.loadFileFromContextPath(filePath));
		Object configObject = JSONDecode.parse(fileContent);
		return TObject.cast(configObject);
	}
	
	/**
	 * 从配置文件初始化 config 对象
	 * @return
	 */
	public static WebServerConfig getWebServerConfig() {
		WebServerConfig config = new WebServerConfig();

		//使用反射工具自动加载
		try {
			config = (WebServerConfig)TReflect.getObjectFromMap(WebServerConfig.class,WEB_CONFIG,true);
		} catch (ReflectiveOperationException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}

		//如果是相对路径则转换成绝对路径
		if(!config.getContextPath().startsWith(File.separator)){
			config.setContextPath(System.getProperty("user.dir")+File.separator+config.getContextPath());
		}
		if(config.getContextPath().endsWith(File.separator)){
			config.setContextPath(TString.removeSuffix(config.getContextPath()));
		}

		//初始化过滤器
		config.addAllFilterConfigs(getContextParameter("Filter",new ArrayList<Map<String,Object>>()));
		
		return config;
	}

	public static void welcome(WebServerConfig config){
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
		if(config.getCertificateFile()!=null) {
			Logger.simple("\tCertificateFile:\t\t" + config.getCertificateFile());
			Logger.simple("\tCertificatePassword:\t" + config.getCertificatePassword());
			Logger.simple("\tKeyPassword:\t\t\t" + config.getKeyPassword());
		}
		Logger.simple("=============================================================================================");
		Logger.simple("Process ID: "+TEnv.getCurrentPID());
		Logger.simple("WebServer working on: "+config.getHost()+":"+config.getPort()+" ...");

	}

	/**
	 * 生成 accessLog 日志
	 * @param request
	 * @param response
	 * @return
	 */
	private static String genAccessLog(HttpRequest request,HttpResponse response){
		StringBuilder content = new StringBuilder();
		content.append("["+TDateTime.now()+"]");
		content.append(" "+TString.rightPad(request.getRemoteAddres(),15,' '));
		content.append(" "+TString.rightPad(request.getRemotePort()+"",5,' '));
		content.append(" "+request.protocol().getProtocol()+"/"+request.protocol().getVersion()+" "+request.protocol().getMethod());
		content.append(" "+response.protocol().getStatus());
		content.append(" "+response.body().getBodyBytes().length);
		content.append("\t "+request.protocol().getPath());
		content.append("\t "+TObject.nullDefault(request.header().get("User-Agent"),""));
		content.append("\t "+TObject.nullDefault(request.header().get("Referer"),""));
		content.append("\r\n");
		return content.toString();
	}
	
	/**
	 * 写入ac'ce
	 * @param request
	 * @param response
	 */
	public static void writeAccessLog(HttpRequest request,HttpResponse response){
		SingleLogger.writeLog(ACCESS_LOG_FILE_NAME,genAccessLog(request, response));
	}
	
	/**
	 * 获取 Web 服务配置
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getContextParameter(String name, T defaultValue) {
		return WEB_CONFIG.get(name) == null ? defaultValue : ((T) WEB_CONFIG.get(name));
	}
	
	/**
	 * 获取 mime 定义
	 * @return
	 */
	public static Map<String, Object> getMimeDefine() {
		return MIME_TYPES;
	}
	
	/**
	 * 获取错误输出定义
	 * @return
	 */
	public static Map<String, Object> getErrorDefine() {
		return ERROR_DEFINE;
	}

	/**
	 * 获取版本号
	 * @return
	 */
	public final static String getVERSION() {
		return VERSION;
	}
	
	/**
	 * 获取在 Cookie 中保存 session id 的名称
	 * @return
	 */
	public static String getSessionName() {
		return SESSION_NAME;
	}
	
	
}
