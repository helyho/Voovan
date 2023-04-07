package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * properties文件操作类
 * 		当properties 文件变更后自动移除缓存内的数据, 下次访问时会重新读取文件内容
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TProperties {

	private static ConcurrentHashMap<File, Properties> propertiesFile = new ConcurrentHashMap<File, Properties>();
	private static ConcurrentHashMap<String, Properties> propertiesName = new ConcurrentHashMap<String, Properties>();
	private static ConcurrentHashMap<File, Long> propertiesWatcher = new ConcurrentHashMap<File, Long>();

	static {
		Global.schedual(new HashWheelTask() {
			@Override
			public void run() {
				Iterator<Map.Entry<File, Long>> iterator = propertiesWatcher.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<File, Long> entry = iterator.next();
					File file = entry.getKey();
					long lastWatchTime = entry.getValue();
					long lastFileTime = file.lastModified();
                    if (lastWatchTime != lastFileTime) {
						propertiesFile.remove(file);
						propertiesName.remove(file.getName());
                        iterator.remove();
                    }
				}
			}

		}, 5, true);
	}


	/**
	 * 从 http 服务拉取并解析 Properties 文件
	 *
	 * @param url 文件对象
	 * @return Properties 对象
	 */
	public static Properties getProperties(URL url) {
		try {
			Properties properites = new Properties();

			Object object = url.getContent();

			String content = new String(TStream.readAll((InputStream)object));
			properites.load(new StringReader(content));
			System.out.println("[PROPERTIES] Load Properties: " + url.toString());

			return properites;

		} catch (IOException e) {
			System.out.println("Load properites file failed. File:" + url.toString() + "-->" + e.getMessage());
			return null;
		}
	}

	/**
	 * 解析 Properties 文件
	 *
	 * @param file 文件对象
	 * @return Properties 对象
	 */
	public static Properties getProperties(File file) {
		try {
			Properties properties = propertiesFile.get(file);

			if (properties==null) {
				Properties properites = new Properties();
				String content = null;
				if(!file.getPath().contains("!"+File.separator)) {
					byte[] contentBytes = TFile.loadFile(file);
					contentBytes = contentBytes == null ? new byte[0] : contentBytes;
					content = new String(contentBytes);
				} else {
					String filePath = file.getPath();
					String resourcePath = filePath.substring(filePath.lastIndexOf("!"+File.separator)+2, filePath.length());
					content = new String(TFile.loadResource(resourcePath));
				}
				properites.load(new StringReader(content));
				propertiesWatcher.put(file, file.lastModified());
				propertiesFile.put(file, properites);
				System.out.println("[PROPERTIES] Load Properties file: " + file.getPath());
			}

			return propertiesFile.get(file);

		} catch (IOException e) {
			System.out.println("Load properites file failed. File:" + file.getAbsolutePath() + "-->" + e.getMessage());
			return null;
		}
	}

	/**
	 * 解析 Properties 文件
	 *
	 * @param fileName 文件名, 不包含扩展名, 或自动瓶装环境参数和扩展名
	 *                 传入 database 参数会拼装出 database-环境名.properties 作为文件名
	 *                 并且在 classes 或者 target/classes 目录下寻找指定文件.
	 *                 如果没有指定环境名的配置文件则使用默认的配置文件
	 * @return Properties 对象
	 */
	public static Properties getProperties(String fileName) {
		Properties properties;

		if(fileName.startsWith("http")) {
			properties = propertiesName.get(fileName);
			if(properties == null) {
				try {
					properties = getProperties(new URL(fileName));
					propertiesName.put(fileName, properties);
				} catch (MalformedURLException e) {
					System.out.println("Load properites failed. url:" + fileName + "-->" + e.getMessage());
					return null;
				}
			}

			return properties;
		} else {
			File file = null;

			String configFileNameWithEnv = null;
			String configFileName = "";
			String envName = TEnv.getEnvName();
			envName = envName == null ? "" : "-" + envName;

			if (!fileName.contains(".properties")) {
				configFileNameWithEnv = fileName + envName + ".properties";
				configFileName = fileName + ".properties";
			} else {
				configFileNameWithEnv = TString.insert(fileName, fileName.indexOf("."), envName);
				configFileName = fileName;
			}

			properties = propertiesName.get(configFileNameWithEnv);
			if (properties == null) {
				properties = propertiesName.get(configFileName);
			}

			if (properties == null) {
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
					properties = getProperties(file);
					propertiesName.put(fileName, properties);
					return properties;
				} else {
					System.out.println("[PROPERTIES] Load properites file failed. File:" + (configFile!=null ? configFile.getAbsolutePath() : "") + " not exists");
					return null;
				}
			} else {
				return properties;
			}
		}
	}

	/**
	 * 从Properties文件读取字符串
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static String getString(File file, String name, String defaultValue) {
		Properties properites = getProperties(file);

		if(properites == null) {
			return defaultValue;
		}

		String value = properites.getProperty(name);
		return TString.isNullOrEmpty(value) ? defaultValue: value;
	}

	/**
	 * 从Properties文件读取字符串
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static String getString(File file, String name) {
		return getString(file, name, null);
	}

	/**
	 * 从Properties文件读取整形
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
	 * 从Properties文件读取整形
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static int getInt(File file, String name) {
		return getInt(file, name, null);
	}

	/**
	 * 从Properties文件读取浮点数
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
	 * 从Properties文件读取浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static float getFloat(File file, String name) {
		return getFloat(file, name, null);
	}

	/**
	 * 从Properties读取双精度浮点数
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
	 * 从Properties读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static double getDouble(File file, String name) {
	    return getDouble(file, name, null);
	}

	/**
	 * 从Properties文件读取 Boolean
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
	 * 从Properties读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static boolean defaultValue(File file, String name) {
		return getBoolean(file, name, null);
	}

	/**
	 * 保存信息到 Properties文件
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param value 属性值
	 * @throws IOException IO异常
	 */
	public static void setString(File file, String name, String value) throws IOException {
		Properties properites = getProperties(file);
		if(properites==null) {
			return;
		}
		properites.setProperty(name, value);
		properites.store(new FileOutputStream(file), null);
	}

	//-----------------------------------------------------------------------------


	/**
	 * 从Properties文件读取字符串
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @param defaultValue 默认值
	 * @return 属性值
	 */
	public static String getString(String fileName, String name, String defaultValue) {
		Properties properites = getProperties(fileName);
		String value = properites==null ? null : properites.getProperty(name);
		return TString.isNullOrEmpty(value) ? defaultValue : value;
	}

	/**
	 * 从Properties文件读取字符串
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static String getString(String fileName, String name) {
		return getString(fileName, name, null);
	}

	/**
	 * 从Properties文件读取整形
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
	 * 从Properties文件读取整形
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static int getInt(String fileName, String name) {
	    return getInt(fileName, name, null);
	}

	/**
	 * 从Properties文件读取浮点数
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
	 * 从Properties文件读取浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static float getFloat(String fileName, String name) {
		return getFloat(fileName, name, null);
	}

	/**
	 * 从Properties读取双精度浮点数
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
	 * 从Properties读取双精度浮点数
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static double getDouble(String fileName, String name) {
	    return getDouble(fileName, name, null);
	}


	/**
	 * 从Properties文件读取 Boolean
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
	 * 从Properties文件读取 Boolean
	 *
	 * @param fileName 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static boolean getBoolean(String fileName, String name) {
		return getBoolean(fileName, name, null);
	}

	/**
	 * 清空 Properites 缓存
	 */
	public void clear(){
		propertiesFile.clear();
		propertiesName.clear();
	}
}
