package org.hocate.http.server;

import java.util.Map;

import org.hocate.json.JSONDecode;
import org.hocate.tools.TFile;
import org.hocate.tools.TObject;

/**
 * Web上下文(配置信息读取)
 * @author helyho
 *
 */
public class WebContext {
	
	private static String sessionName = "SESSIONID";

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
	private static final String version = "0.1";
															
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
	
	
	public static WebConfig getWebConfig() {
		WebConfig config = new WebConfig();
		config.setHost(getContextParameter("Host","0.0.0.0"));
		config.setPort(getContextParameter("Port",8080));
		config.setTimeout(getContextParameter("Timeout",3000));
		config.setContextPath(getContextParameter("ContextPath",System.getProperty("user.dir")));
		config.setCharacterSet(getContextParameter("CharacterSet","UTF-8"));
		config.setSessionContainer(getContextParameter("SessionContainer","java.util.Hashtable"));
		config.setSessionTimeout(getContextParameter("SessionTimeout",30));
		config.setKeepAliveTimeout(getContextParameter("KeepAliveTimeout",5));
		return config;
	}

	
	/**
	 * 获取 Web 服务配置
	 * @param <T>
	 * @return
	 */
	public static <T> T getContextParameter(String name,T defaultValue) {
		return webConfig.get(name)==null?defaultValue:TObject.cast(webConfig.get(name));
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
		return version;
	}
	
	/**
	 * 获取在 Cookie 中保存 session id 的名称
	 * @return
	 */
	public static String getSessionName() {
		return sessionName;
	}
}
