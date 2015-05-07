package org.voovan.tools.json;

/**
 * JAVA 对象和 JSON 对象转换类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSON {
	
	/**
	 * 将 Java 对象 转换成 JSON字符串
	 * @param object   	待转换的对象
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSON(Object object){
		String jsonString = null;
		try {
			jsonString = JSONEncode.fromObject(object);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return jsonString;
	}
	
	/**
	 * 将 JSON字符串 转换成 Java 对象
	 * @param <T>
	 * @param jsonStr		待转换的 JSON 字符串
	 * @param clazz			转换的目标 java 类
	 * @return				转换后的 Java 对象
	 */
	public static <T> T toObject(String jsonStr,Class<T> clazz){
		T valueObject = null;
		try {
			valueObject = JSONDecode.fromJSON(jsonStr, clazz);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return valueObject;
	}
	
	
	/**
	 * 解析 JSON 字符串
	 * 		如果是{}包裹的字符串解析成 HashMap,如果是[]包裹的字符串解析成 ArrayList
	 * @param jsonStr	待解析的 JSON 字符串
	 * @return
	 */
	public static Object parse(String jsonStr){
		Object parseObject = null;
		try {
			parseObject = JSONDecode.parse(jsonStr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return parseObject;
	}
}
