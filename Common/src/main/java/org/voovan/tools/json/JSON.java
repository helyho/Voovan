package org.voovan.tools.json;

import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.function.Supplier;

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
	 * 是否进行 EscapeChar 的转换, 默认 true, 当你确定你不存在字符串多行换行的时候可设置为 true
	 */
	private static ThreadLocal<Boolean> convertEscapeChar = ThreadLocal.withInitial(new Supplier<Boolean>() {
		@Override
		public Boolean get() {
			return true;
		}
	});

	/**
	 * 是否进行 EscapeChar 的转换
	 * @return true: 是, false: 否
	 */
	public static boolean isConvertEscapeChar() {
		return convertEscapeChar.get();
	}

	/**
	 * 设置是否进行 EscapeChar 的转换
	 * @return true: 是, false: 否
	 */
	public static void setConvertEscapeChar(boolean convertEscapeChar) {
		JSON.convertEscapeChar.set(convertEscapeChar);
	}

	/**
	 * 将 Java 对象 转换成 JSON字符串
	 * @param object   	待转换的对象
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSON(Object object){
		return toJSON(object, true);
	}


	/**
	 * 将 Java 对象 转换成 JSON字符串
	 * @param object   	待转换的对象
	 * @param convertEscapeChar 是否转换转义字符
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSON(Object object, boolean convertEscapeChar){
		String jsonString = null;
		try {
			//保存旧的 convertEscapeChar 标志
			boolean oldConvertEscapeChar = isConvertEscapeChar();

			//设置新的 convertEscapeChar 标志
			setConvertEscapeChar(convertEscapeChar);
			jsonString = JSONEncode.fromObject(object);

			//恢复旧的 convertEscapeChar 标志
			setConvertEscapeChar(oldConvertEscapeChar);
		} catch (ReflectiveOperationException e) {
			Logger.error("Reflective Operation failed",e);
		}
		return jsonString;
	}

	/**
	 * 将 JSON字符串 转换成 Java 对象
	 * @param <T>			范型
	 * @param jsonStr		待转换的 JSON 字符串
	 * @param type			转换的目标 java 类
	 * @param ignoreCase    是否忽略字段大小写
	 * @return				转换后的 Java 对象
	 */
	public static <T> T toObject(String jsonStr, Type type, boolean ignoreCase){
		T valueObject = null;
		try {
			valueObject = JSONDecode.fromJSON(jsonStr, type, ignoreCase);
		} catch (ReflectiveOperationException | ParseException e) {
			Logger.error("Reflective Operation failed",e);
		}
		return valueObject;
	}


	/**
	 * 将 JSON字符串 转换成 Java 对象,默认严格限制字段大小写
	 * @param <T>			范型
	 * @param jsonStr		待转换的 JSON 字符串
	 * @param type			转换的目标 java 类
	 * @return				转换后的 Java 对象
	 */
	public static <T> T toObject(String jsonStr,Type type){
		return toObject(jsonStr, type , false);
	}


	/**
	 * 解析 JSON 字符串
	 * 		如果是{}包裹的字符串解析成 HashMap,如果是[]包裹的字符串解析成 ArrayList
	 * @param jsonStr	待解析的 JSON 字符串
	 * @return 接口后的对象
	 */
	public static Object parse(String jsonStr){
		Object parseObject = null;
		parseObject = JSONDecode.parse(jsonStr);
		return parseObject;
	}

	/**
	 * 格式化 JSON
	 * @param jsonStr JSON 字符串
	 * @return  格式化后的 JSON 字符串
	 */
	public static String formatJson(String jsonStr) {
		if (TString.isNullOrEmpty(jsonStr)){
			return "";
		}
		StringBuilder jsongStrBuild = new StringBuilder();
		char prevChar = '\0';
		char current = '\0';
		int indent = 0;
		boolean inStr = false;
		for (int i = 0; i < jsonStr.length(); i++) {
			prevChar = current;
			current = jsonStr.charAt(i);

			//判断是否在字符串中
			if(current == '\"' && prevChar!='\\'){
				inStr = !inStr;
			}

			if(inStr){
				jsongStrBuild.append(current);
				continue;
			}

			if(current=='[' || current=='{'){
				jsongStrBuild.append(current);
				jsongStrBuild.append('\n');
				indent++;
				addIndentByNum(jsongStrBuild, indent);
				continue;
			}

			if(current==']' || current=='}'){
				jsongStrBuild.append('\n');
				indent--;
				addIndentByNum(jsongStrBuild, indent);
				jsongStrBuild.append(current);
				continue;
			}

			if(current==','){
				jsongStrBuild.append(current);
				jsongStrBuild.append('\n');
				addIndentByNum(jsongStrBuild, indent);
				continue;
			}

			jsongStrBuild.append(current);

		}

		return jsongStrBuild.toString();
	}

	/**
	 * 添加缩进
	 * @param str     需要追加缩进的字符串
	 * @param indent  缩进后的字符串
	 */
	private static void addIndentByNum(StringBuilder  str, int indent) {
		for (int i = 0; i < indent; i++) {
			str.append('\t');
		}
	}

	/**
	 * 清理json字符串串null节点
	 * @param jsonStr json 字符串
	 * @return 清理null节点的结果
	 */
	public static String removeNullNode(String jsonStr){
		jsonStr	= TString.fastReplaceAll(jsonStr, "\\\"\\w+?\\\":null", "");
		jsonStr	= TString.fastReplaceAll(jsonStr, "null", "");
		return fixJSON(jsonStr);
	}

	/**
	 * 修复 JSON 字符串中因清理节点导致的多个","的分割异常问题
	 * @param jsonStr json 字符串
	 * @return 清理后点的结果
	 */
	protected static String fixJSON(String jsonStr){

		while(TString.searchByRegex(jsonStr,",[\\s\\r\\n]*,").length>0) {
			jsonStr = TString.fastReplaceAll(jsonStr, ",[\\s\\r\\n]*,", ",");
		}

		jsonStr	= TString.fastReplaceAll(jsonStr, "(?:[\\{])[\\s\\r\\n]*,","{");
		jsonStr	= TString.fastReplaceAll(jsonStr, "(?:[\\[])[\\s\\r\\n]*,","[");
		jsonStr	= TString.fastReplaceAll(jsonStr, ",[\\s\\r\\n]*(?:[\\}])","}");
		jsonStr	= TString.fastReplaceAll(jsonStr, ",[\\s\\r\\n]*(?:[\\]])","]");

		return jsonStr;
	}

	/**
	 * 判断是否是 JSON 的 map 类型
	 * @param jsonStr 目标字符串
	 * @return true: 是, false: 否
	 */
	public static boolean isJSONMap(String jsonStr){
		return TString.searchByRegex(jsonStr, "^\\s*\\{[\\s\\S]*\\}\\s*$").length > 0;
	}

	/**
	 * 判断是否是 JSON 的 list/array 类型
	 * @param jsonStr 目标字符串
	 * @return true: 是, false: 否
	 */
	public static boolean isJSONList(String jsonStr){
		return TString.searchByRegex(jsonStr, "^\\s*\\[[\\s\\S]*\\]\\s*$").length > 0;
	}

	/**
	 * 判断是否是 JSON 的可解析
	 * @param jsonStr 目标字符串
	 * @return true: 是, false: 否
	 */
	public static boolean isJSON(String jsonStr){
		return isJSONMap(jsonStr) || isJSONList(jsonStr);
	}
}
