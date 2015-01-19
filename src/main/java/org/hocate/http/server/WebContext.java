package org.hocate.http.server;

import java.util.Map;

import org.hocate.json.JSONDecode;
import org.hocate.tools.TFile;
import org.hocate.tools.TObject;
import org.hocate.tools.TReflect;

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
	
	
	/**
	 * 获取 Session 容器
	 */
	public static Map<String, HttpSession> getSessionConatiner(){
		try {
			String className = WebContext.getWebConfig("SessionContainer","java.util.Hashtable");
			Class<?> sessionContainerClass = Class.forName(className);
			Map<String, HttpSession> sessionContainer = TObject.cast(TReflect.newInstance(sessionContainerClass));
			return sessionContainer;
		} catch (Exception e) {
			return null;
		}
	}
	
	/**
	 * 获取 Web 服务配置
	 * @param <T>
	 * @return
	 */
	public static <T> T getWebConfig(String name,T defaultValue) {
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
