package org.voovan.tools;

import org.voovan.tools.log.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;

/**
 * properties文件操作类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TProperties {

	private static HashMap<File, Properties>	propertiesCache	= new HashMap<File, Properties>();

	/**
	 * 解析 Properties 文件
	 *
	 * @param file 文件对象
	 * @return Properties 对象
	 */
	public static Properties getProperties(File file) {
		try {
			if (!propertiesCache.containsKey(file)) {
				Properties properites = new Properties();
				String content = null;
				if(!file.getPath().contains("!"+File.separator)) {
					content = new String(TFile.loadFile(file));
				}else{
					String filePath = file.getPath();
					String resourcePath = filePath.substring(filePath.indexOf("!"+File.separator)+2, filePath.length());
					content = new String(TFile.loadResource(resourcePath));
				}
				properites.load(new StringReader(content));
				propertiesCache.put(file, properites);
			}

			return propertiesCache.get(file);

		} catch (IOException e) {
			Logger.error("Get properites file fialed. File:"+file.getAbsolutePath(),e);
			return null;
		}
	}

	/**
	 * 从Properties文件读取字符串
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static String getString(File file, String name) {
		Properties properites = getProperties(file);
		return properites.getProperty(name);
	}

	/**
	 * 从Properties文件读取整形
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static int getInt(File file, String name) {
		String value = getString(file, name);
		return Integer.valueOf(value==null?"0":value.trim());
	}

	/**
	 * 从Properties文件读取浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static float getFloat(File file, String name) {
		String value = getString(file, name);
		return Float.valueOf(value==null?"0":value.trim());
	}

	/**
	 * 从Properties读取双精度浮点数
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @return 属性值
	 */
	public static double getDouble(File file, String name) {
		String value = getString(file, name);
		return Double.valueOf(value==null?"0":value.trim());
	}

	/**
	 * 保存信息到 Properties文件
	 *
	 * @param file 文件对象
	 * @param name 属性名
	 * @param name 属性值
	 */
	public static void setString(File file, String name, String value) throws IOException {
		Properties properites = getProperties(file);
		properites.setProperty(name, value);
		properites.store(new FileOutputStream(file), null);
	}

	/**
	 * 清空 Properites 缓存
	 */
	public void clear(){
		propertiesCache.clear();
	}
}
