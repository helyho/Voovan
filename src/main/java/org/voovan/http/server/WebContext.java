package org.voovan.http.server;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.log.Logger;
import org.voovan.tools.threadpool.ThreadPool;

/**
 * Web上下文(配置信息读取)
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebContext {
	
	private static String sessionName = "SESSIONID";
	
	private static ThreadPoolExecutor threadPool = ThreadPool.getThreadPool();

	/**
	 * Web Config
	 */
	private static Map<String, Object>	webConfig	= loadMapFromFile("/Config/web.js");

	/**
	 * MimeMap
	 */
	private static Map<String, Object>	mimeTypes	= loadMapFromFile("/Config/mime.js");
	/**
	 * 错误输出 Map
	 */
	private static Map<String, Object>	errorDefine	= loadMapFromFile("/Config/error.js");
	/**
	 * 当前版本号
	 */
	private static final String VERSION = "0.1";
	
	private WebContext(){
		
	}
	
	/**
	 * 返回公用线程池
	 * @return
	 */
	public static ThreadPoolExecutor getThreadPool(){
		return threadPool;
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
		config.setHost(getContextParameter("Host","127.0.0.1"));
		config.setPort(getContextParameter("Port",8080));
		config.setTimeout(getContextParameter("Timeout",3*1000));
		config.setContextPath(getContextParameter("ContextPath",System.getProperty("user.dir")));
		config.setCharacterSet(getContextParameter("CharacterSet","UTF-8"));
		config.setSessionContainer(getContextParameter("SessionContainer","java.util.Hashtable"));
		config.setSessionTimeout(getContextParameter("SessionTimeout",30));
		config.setKeepAliveTimeout(getContextParameter("KeepAliveTimeout",5));
		config.setGzip(getContextParameter("Gzip","on").equals("on")?true:false);
		
		Logger.simple("\r\n");
		Logger.simple("==            ==  ==========   ==========  ==            ==  ====       ==  ==       ==");
		Logger.simple(" ==          ==  ==        == ==        ==  ==          ==  ==  ==      ==   ==      =="); 
		Logger.simple("  ==        ==   ==        == ==        ==   ==        ==  ==    ==     ==    ==     =="); 
		Logger.simple("   ==      ==    ==        == ==        ==    ==      ==  ==      ==    ==     ==    =="); 
		Logger.simple("    ==    ==     ==        == ==        ==     ==    ==  ============   ==      ==   =="); 
		Logger.simple("     ==  ==      ==        == ==        ==      ==  ==  ==          ==  ==       ==  =="); 
		Logger.simple("      ====        ==========   ==========        ====  ==            == ==        == ==");
		Logger.simple("");
		Logger.simple("===========================Config parameter list===========================");
		Logger.simple("\tTimeout:\t\t"+config.getTimeout());
		Logger.simple("\tContextPath:\t\t"+config.getContextPath());
		Logger.simple("\tCharacterSet:\t\t"+config.getCharacterSet());
		Logger.simple("\tSessionContainer:\t"+config.getSessionContainer());
		Logger.simple("\tSessionTimeout:\t\t"+config.getSessionTimeout());
		Logger.simple("\tKeepAliveTimeout:\t"+config.getKeepAliveTimeout());
		Logger.simple("\tGzip:\t\t\t"+ (config.isGzip()?"on":"off"));
		Logger.simple("===========================================================================");
		Logger.simple("WebServer working on:"+config.getHost()+":"+config.getPort()+" ...");
		//初始化过滤器
		config.addAllFilterConfigs(getContextParameter("Filter",new ArrayList<Map<String,Object>>()));
		
		return config;
	}

	
	/**
	 * 获取 Web 服务配置
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getContextParameter(String name, T defaultValue) {
		return webConfig.get(name) == null ? defaultValue : ((T) webConfig.get(name));
	}
	
	/**
	 * 获取 mime 定义
	 * @return
	 */
	public static Map<String, Object> getMimeDefine() {
		return mimeTypes;
	}
	
	/**
	 * 获取错误输出定义
	 * @return
	 */
	public static Map<String, Object> getErrorDefine() {
		return errorDefine;
	}

	/**
	 * 获取版本号
	 * @return
	 */
	public final static String getVersion() {
		return VERSION;
	}
	
	/**
	 * 获取在 Cookie 中保存 session id 的名称
	 * @return
	 */
	public static String getSessionName() {
		return sessionName;
	}
}
