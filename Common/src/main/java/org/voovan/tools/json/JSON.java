package org.voovan.tools.json;

import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.TFile;
import org.voovan.tools.TProperties;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
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
	public final static String JSON_CONVERT_ESCAPE_CHAR = TProperties.getString("framework", "JSONConvertEscapeChar", "false");

	/**
	 * 是否进行 EscapeChar 的转换, 默认 true, 当你确定你不存在字符串多行换行的时候可设置为 false
	 */
	private static FastThreadLocal<Boolean> convertEscapeChar = FastThreadLocal.withInitial(new Supplier<Boolean>() {
		@Override
		public Boolean get() {
			boolean isEscapeChare = true;

			if("true".equalsIgnoreCase(JSON_CONVERT_ESCAPE_CHAR.trim())) {
				isEscapeChare = true;
			} if("false".equalsIgnoreCase(JSON_CONVERT_ESCAPE_CHAR.trim())) {
				isEscapeChare = false;
			} else {
				isEscapeChare = false;
			}

			return isEscapeChare;
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
	 * @param convertEscapeChar true: 是, false: 否
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
		return toJSON(object, convertEscapeChar.get(), false);
	}

	/**
	 * 将 Java 对象 转换成 JSON字符串
	 * @param object   	待转换的对象
	 * @param allField  是否序列化所有的属性
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSON(Object object, boolean allField){
		return toJSON(object, convertEscapeChar.get(), allField);
	}

	/**
	 * 将 Java 对象 转换成 JSON字符串, 并格式化
	 * @param object   	待转换的对象
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSONWithFormat(Object object){
		return formatJson(toJSON(object, convertEscapeChar.get(), false));
	}

	/**
	 * 将 Java 对象 转换成 JSON字符串, 并格式化
	 * @param object   	待转换的对象
	 * @param allField  是否序列化所有的属性
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSONWithFormat(Object object, boolean allField){
		return formatJson(toJSON(object, convertEscapeChar.get(), allField));

	}

	/**
	 * 将 Java 对象 转换成 JSON字符串
	 * @param object   			待转换的对象
	 * @param convertEscapeChar 是否转换转义字符
	 * @param allField  		是否序列化所有的属性
	 * @return			转换后的 JSON 字符串
	 */
	public static String toJSON(Object object, boolean convertEscapeChar, boolean allField){
		String jsonString = null;
		try {
			//保存旧的 convertEscapeChar 标志
			boolean oldConvertEscapeChar = isConvertEscapeChar();

			//设置新的 convertEscapeChar 标志
			setConvertEscapeChar(convertEscapeChar);
			if(allField) {
				jsonString = JSONEncode.fromObject(TReflect.getMapFromObject(object, allField), allField);
			} else {
				jsonString = JSONEncode.fromObject(object, false);
			}

			if(jsonString.startsWith("\"") && jsonString.endsWith("\"")){
				jsonString = TString.removeSuffix(jsonString);
				jsonString = TString.removePrefix(jsonString);
			}

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
	 * 将 JSON字符串 转换成 Java 对象
	 * @param <T>			范型
	 * @param file			JSON 文件
	 * @param type			转换的目标 java 类
	 * @param ignoreCase    是否忽略字段大小写
	 * @return				转换后的 Java 对象
	 */
	public static <T> T toObject(File file, Type type, boolean ignoreCase) {
		if(file.exists()) {
			String fileContent = null;
			try {
				fileContent = new String(TFile.loadFile(file),"UTF-8");
				if(fileContent != null) {
					return toObject(fileContent, type, ignoreCase);
				}
			} catch (UnsupportedEncodingException e) {
				Logger.error(e);
			}
		}
		return null;
	}


	/**
	 * 将 JSON字符串 转换成 Java 对象
	 * @param <T>			范型
	 * @param file			JSON 文件
	 * @param type			转换的目标 java 类
	 * @return				转换后的 Java 对象
	 */
	public static <T> T toObject(File file, Type type) {
		return toObject(file, type, true);
	}

	/**
	 * 解析 JSON 字符串
	 * 		如果是{}包裹的字符串解析成 HashMap,如果是[]包裹的字符串解析成 ArrayList
	 * @param jsonStr	待解析的 JSON 字符串
	 * @return 接口后的对象
	 */
	public static Object parse(String jsonStr){
		if(jsonStr==null) {
			return null;
		}

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

			if(current==':'){
				jsongStrBuild.append(current);
				jsongStrBuild.append(' ');
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
		jsonStr	= TString.fastReplaceAll(jsonStr, "\\\".\\w+?\\\":\\s*null\\s*,?[\\s]*", "");
    	return fixJSON(jsonStr);
	}

	/**
	 * 不考虑层级, 清除 JSON 中指定的 field
	 * @param jsonStr json 字符串
	 * @param fields 被清除的 field
	 * @return 清理 field 后的结果
	 */
	public static String removeNode(String jsonStr, String ... fields) {
		for(String field : fields) {
			jsonStr = TString.fastReplaceAll(jsonStr, "\\\"" + field + "\\\":.*,?(?=|\\]|\\})[\\s]*", "");
		}

		return fixJSON(jsonStr);
	}

	/**
	 * 替换 json 中的节点
	 * @param jsonStr json 字符串
	 * @param field 被替换的属性名
	 * @param obj 替换的对象
	 * @return 替换后的结果
	 */
	public static String replaceNode(String jsonStr, String field, Object obj) {
		String nodeJson = JSON.toJSON(obj);
		if(nodeJson.charAt(0) == '{' && nodeJson.charAt(nodeJson.length() - 1) == '}') {
			nodeJson = TString.removePrefix(nodeJson);
			nodeJson = TString.removeSuffix(nodeJson);
			jsonStr = TString.fastReplaceAll(jsonStr, "\\\"" + field + "\\\":.*(?:[^,\\s])", nodeJson);

		}
		return jsonStr;
	}

	/**
	 * 收缩 JSON 字符串
	 * 		移除制表符空格等不可见字符
	 * @param jsonStr 被收缩的字符串
	 * @param withLineSeparator 是否移除换行符
	 * @return 收缩后的 JSON 字符串
	 */
	public static String shrink(String jsonStr, boolean withLineSeparator) {
		if(withLineSeparator) {
			jsonStr = TString.fastReplaceAll(jsonStr, "\\s*[\r\n]\\s*", "");
		}

		jsonStr = TString.fastReplaceAll(jsonStr, "\\s*([\\,\\:\\{\\}\\[\\]])\\s*","$1", false);

		return jsonStr;
	}

	/**
	 * 修复 JSON 字符串中因清理节点导致的多个","的分割异常问题
	 * @param jsonStr json 字符串
	 * @return 清理后点的结果
	 */
	public static String fixJSON(String jsonStr){

		while(TString.regexMatch(jsonStr,",[\\s]*,") > 0) {
			jsonStr = TString.fastReplaceAll(jsonStr, ",[\\s]*,", ",");
		}

		jsonStr	= TString.fastReplaceAll(jsonStr, "(?:[\\{])[\\s]*,","{");
		jsonStr	= TString.fastReplaceAll(jsonStr, "(?:[\\{])[\\s]*,","{");
		jsonStr	= TString.fastReplaceAll(jsonStr, "(?:[\\[])[\\s]*,","[");
		jsonStr	= TString.fastReplaceAll(jsonStr, ",[\\s]*(?:[\\}])","}");
		jsonStr	= TString.fastReplaceAll(jsonStr, ",[\\s]*(?:[\\]])","]");

		return jsonStr;
	}

	/**
	 * 判断是否是 JSON 的 map 类型
	 * @param jsonStr 目标字符串
	 * @return true: 是, false: 否
	 */
	public static boolean isJSONMap(String jsonStr){
		return TString.regexMatch(jsonStr, "^\\s*\\{[\\s\\S]*\\}\\s*$") > 0;
	}

	/**
	 * 判断是否是 JSON 的 list/array 类型
	 * @param jsonStr 目标字符串
	 * @return true: 是, false: 否
	 */
	public static boolean isJSONList(String jsonStr){
		return TString.regexMatch(jsonStr, "^\\s*\\[[\\s\\S]*\\]\\s*$") > 0;
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
