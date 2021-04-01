package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.json.JSONDecode;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * conf 文件操作类
 * 		当 conf 文件变更后自动移除缓存内的数据, 下次访问时会重新读取文件内容
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TConfig {

	private static ConcurrentHashMap<File, Map<String, Object>> configFile = new ConcurrentHashMap<File, Map<String, Object>>();
	private static ConcurrentHashMap<String, Map<String, Object>> configName = new ConcurrentHashMap<String, Map<String, Object>>();
	private static ConcurrentHashMap<File, Long> configWatcher = new ConcurrentHashMap<File, Long>();

	static {
		Global.getHashWheelTimer().addTask(new HashWheelTask() {
			@Override
			public void run() {
				Iterator<Map.Entry<File, Long>> iterator = configWatcher.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<File, Long> entry = iterator.next();
					File file = entry.getKey();
					long lastWatchTime = entry.getValue();
					long lastFileTime = file.lastModified();
                    if (lastWatchTime != lastFileTime) {
						configFile.remove(file);
						configName.remove(file.getName());
                        iterator.remove();
                    }
				}
			}

		}, 5, true);
	}

	/**
	 * 从 http 服务拉取并解析 conf 文件
	 *
	 * @param url 文件对象
	 * @return Map[String, Object] 对象
	 */
	public static Map<String, Object> getConfigs(URL url) {
		try {
			Map<String, Object> configs = new ConcurrentHashMap<String, Object>();

			Object object = url.getContent();

			String content = new String(TStream.readAll((InputStream)object));
			configs = (Map)JSONDecode.parse(content);
			System.out.println("[CONF]  Load configs: " + url.toString());

			return configs;

		} catch (IOException e) {
			System.out.println("[CONF] Load configs file failed. File:" + url.toString() + "-->" + e.getMessage());
			return null;
		}
	}

	/**
	 * 解析 conf 文件
	 *
	 * @param file 文件对象
	 * @return Map[String, Object] 对象
	 */
	public static Map<String, Object> getConfigs(File file) {
		Map<String, Object> configs = configFile.get(file);

		if (configs==null) {
			configs = new ConcurrentHashMap<String, Object>();
			String content = null;
			if(!file.getPath().contains("!"+File.separator)) {
				byte[] contentBytes = TFile.loadFile(file);
				contentBytes = contentBytes == null ? new byte[0] : contentBytes;
				content = new String(contentBytes);
			}else{
				String filePath = file.getPath();
				String resourcePath = filePath.substring(filePath.lastIndexOf("!"+File.separator)+2, filePath.length());
				content = new String(TFile.loadResource(resourcePath));
			}
			configs = (Map)JSONDecode.parse(content);
			configWatcher.put(file, file.lastModified());
			configFile.put(file, configs);
			System.out.println("[CONF] Load configs file: " + file.getPath());
		}

		return configs;
	}

	/**
	 * 解析 conf 文件
	 *
	 * @param fileName 文件名, 不包含扩展名, 或自动瓶装环境参数和扩展名
	 *                 传入 database 参数会拼装出 database-环境名.conf 作为文件名
	 *                 并且在 classes 或者 target/classes 目录下寻找指定文件.
	 *                 如果没有指定环境名的配置文件则使用默认的配置文件
	 * @return Map[String, Object] 对象
	 */
	public static Map<String, Object> getConfigs(String fileName) {
		Map<String, Object> configs;

		if(fileName.startsWith("http")) {
			configs = configName.get(fileName);
			if(configs == null) {
				try {
					configs = getConfigs(new URL(fileName));
					configName.put(fileName, configs);
				} catch (MalformedURLException e) {
					System.out.println("[CONF] Load configs failed. url:" + fileName + "-->" + e.getMessage());
					return null;
				}
			}

			return configs;
		} else {
			File file = null;

			String configFileNameWithEnv = null;
			String configFileName = "";
			String envName = TEnv.getEnvName();
			envName = envName == null ? "" : "-" + envName;

			if (!fileName.contains(".conf")) {
				configFileNameWithEnv = fileName + envName + ".conf";
				configFileName = fileName + ".conf";
			} else {
				configFileNameWithEnv = TString.insert(fileName, fileName.indexOf("."), envName);
				configFileName = fileName;
			}

			configs = configName.get(configFileNameWithEnv);
			if (configs == null) {
				configs = configName.get(configFileName);
			}

			if (configs == null) {
				File configFile = TFile.getResourceFile(configFileName);
				File configFileWithEnv = TFile.getResourceFile(configFileNameWithEnv);

				if (configFileWithEnv != null) {
					file = configFileWithEnv;
					fileName = configFileNameWithEnv;
				} else if (configFile != null) {
					file = configFile;
					fileName = configFileName;
				}

				if (file != null) {
					configs = getConfigs(file);
					configName.put(fileName, configs);
					return configs;
				} else {
					System.out.println("[CONF] Load configsconfigs file failed. File:" + (configFile!=null ? configFile.getAbsolutePath() : "") + " not exists");
					return null;
				}
			} else {
				return configs;
			}
		}
	}

	/**
	 * 从conf文件读取字符串
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static String getString(File file, String name, String defaultValue) {
		Map<String, Object> configs = getConfigs(file);
		Object valueObj = configs==null || configs.size()==0 ? null : configs.get(name);
		String value = valueObj == null? null : valueObj.toString();
		return TString.isNullOrEmpty(value) ? defaultValue: value;
	}

	/**
	 * 从conf文件读取字符串
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static String getString(File file, String name) {
		return getString(file, name, null);
	}

	/**
	 * 从conf文件读取整形
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static int getInt(File file, String name, Integer defaultValue) {
		defaultValue = defaultValue == null ? 0 : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Integer.valueOf(value.trim());
	}

	/**
	 * 从conf文件读取整形
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static int getInt(File file, String name) {
		return getInt(file, name, null);
	}

	/**
	 * 从conf文件读取浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static float getFloat(File file, String name, Float defaultValue) {
		defaultValue = defaultValue == null ? 0f : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Float.valueOf(value.trim());
	}

	/**
	 * 从conf文件读取浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static float getFloat(File file, String name) {
		return getFloat(file, name, null);
	}

	/**
	 * 从conf读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static double getDouble(File file, String name, Double defaultValue) {
		defaultValue = defaultValue == null ? 0d : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Double.valueOf(value.trim());
	}

	/**
	 * 从conf读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static double getDouble(File file, String name) {
	    return getDouble(file, name, null);
	}

	/**
	 * 从conf文件读取 Boolean
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static boolean getBoolean(File file, String name, Boolean defaultValue) {
		defaultValue = defaultValue == null ? false : defaultValue;
		String value = getString(file, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Boolean.valueOf(value.trim());
	}

	/**
	 * 从conf读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static boolean defaultValue(File file, String name) {
		return getBoolean(file, name, null);
	}

	/**
	 * 从conf文件读取字符串
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static String getString(String fileName, String name, String defaultValue) {
		Map<String, Object> configs = getConfigs(fileName);
		Object valueObj = configs==null || configs.size()==0 ? null : configs.get(name);
		String value = valueObj == null? null : valueObj.toString();
		return TString.isNullOrEmpty(value) ? defaultValue : value;
	}

	/**
	 * 从conf文件读取字符串
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static String getString(String fileName, String name) {
		return getString(fileName, name, null);
	}

	/**
	 * 从conf文件读取整形
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static int getInt(String fileName, String name, Integer defaultValue) {
		defaultValue = defaultValue == null ? 0 : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Integer.valueOf(value.trim());
	}

	/**
	 * 从conf文件读取整形
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static int getInt(String fileName, String name) {
	    return getInt(fileName, name, null);
	}

	/**
	 * 从conf文件读取浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static float getFloat(String fileName, String name, Float defaultValue) {
		defaultValue = defaultValue == null ? 0f : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Float.valueOf(value.trim());
	}

	/**
	 * 从conf文件读取浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static float getFloat(String fileName, String name) {
		return getFloat(fileName, name, null);
	}

	/**
	 * 从conf读取双精度浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static double getDouble(String fileName, String name, Double defaultValue) {
		defaultValue = defaultValue == null ? 0d : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Double.valueOf(value.trim());
	}

	/**
	 * 从conf读取双精度浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static double getDouble(String fileName, String name) {
	    return getDouble(fileName, name, null);
	}


	/**
	 * 从conf文件读取 Boolean
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static boolean getBoolean(String fileName, String name, Boolean defaultValue) {
		defaultValue = defaultValue == null ? false : defaultValue;
		String value = getString(fileName, name);
		return TString.isNullOrEmpty(value) ? defaultValue : Boolean.valueOf(value.trim());
	}

	/**
	 * 从conf文件读取 Boolean
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static boolean getBoolean(String fileName, String name) {
		return getBoolean(fileName, name, null);
	}

	/**
	 * 清空 conf 缓存
	 */
	public void clear(){
		configFile.clear();
		configName.clear();
	}
}
