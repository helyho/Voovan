package org.hocate.http.server;

import java.util.Map;

import org.hocate.json.JSONDecode;
import org.hocate.tools.TFile;
import org.hocate.tools.TObject;

/**
 * 读取配置
 * @author helyho
 *
 */
public class Config {
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
	 * 获取 mime 定义
	 * @return
	 */
	public static Map<String, Object> mimeDefine() {
		return mimeTypes;
	}
	
	/**
	 * 获取错误输出定义
	 * @return
	 */
	public static Map<String, Object> errorDefine() {
		return errorDefine;
	}

	/**
	 * 获取版本号
	 * @return
	 */
	public final static String getVersion() {
		return version;
	}
}
