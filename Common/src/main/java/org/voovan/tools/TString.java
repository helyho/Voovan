package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.GenericInfo;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String 工具类
 *
 * @author helyho
 * <p>
 * Voovan Framework
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TString {

	private static Map<Integer, Pattern> REGEX_PATTERN = new ConcurrentHashMap<Integer, Pattern>();

	/**
	 * 单词首字母大写
	 *
	 * @param source 字符串
	 * @return 首字母大写后的字符串
	 */
	public static String upperCaseHead(String source) {
		if (source == null) {
			return null;
		}
		char[] charArray = source.toCharArray();
		charArray[0] = Character.toUpperCase(charArray[0]);
		return new String(charArray);
	}


	/**
	 * 移除字符串前缀
	 *
	 * @param source 目标字符串
	 * @return 移除第一个字节后的字符串
	 */
	public static String removePrefix(String source) {
		if (source == null) {
			return null;
		}
		return source.substring(1, source.length());
	}

	/**
	 * 移除字符串后缀
	 *
	 * @param source 目标字符串
	 * @return 移除最后一个字节后的字符串
	 */
	public static String removeSuffix(String source) {
		if (source == null) {
			return null;
		}

		if (source.isEmpty()) {
			return source;
		}
		return source.substring(0, source.length() - 1);
	}

	/**
	 * 左补齐
	 *
	 * @param source 目标字符串
	 * @param len    补齐后字符串的长度
	 * @param c      用于补齐的字符串
	 * @return 补齐后的字符串
	 */
	public static String leftPad(String source, int len, char c) {
		if (source == null) {
			source = "";
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < len - source.length(); i++) {
			sb.append(c);
		}
		return sb.append(source).toString();
	}

	/**
	 * 右补齐
	 *
	 * @param source 目标字符串
	 * @param len    补齐后字符串的长度
	 * @param c      用于补齐的字符串
	 * @return 补齐后的字符串
	 */
	public static String rightPad(String source, int len, char c) {
		if (source == null) {
			source = "";
		}
		StringBuffer sb = new StringBuffer();
		sb.append(source);
		for (int i = 0; i < len - source.length(); i++) {
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * 判断是否是指定进制的数字字符串
	 *
	 * @param numberString 目标字符串
	 * @param radix        进制
	 * @return 是否是指定进制的数字字符串
	 */
	public static boolean isNumber(String numberString, int radix) {
		if (numberString == null) {
			return false;
		}

		try {
			Integer.parseInt(numberString, radix);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 判断是否是整形数
	 *
	 * @param integerString 数字字符串
	 * @return 是否是整形数
	 */
	public static boolean isInteger(String integerString) {
		if (integerString != null && regexMatch(integerString, "^-?[0-9]\\d*$") > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断是否是浮点数
	 *
	 * @param decimalString 浮点数字符串
	 * @return 是否是浮点数
	 */
	public static boolean isDecimal(String decimalString) {
		if (decimalString != null && regexMatch(decimalString, "^-?\\d+\\.\\d+$") > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断是否是布尔值
	 *
	 * @param booleanString 布尔值字符串
	 * @return 是否是布尔值
	 */
	public static boolean isBoolean(String booleanString) {
		if ("true".equalsIgnoreCase(booleanString) || "false".equalsIgnoreCase(booleanString)) {
			return true;
		} else {
			return false;
		}
	}

	private static Pattern getCachedPattern(String regex, Integer flags) {
		Pattern pattern = null;
		flags = flags == null ? 0 : flags;
		pattern = REGEX_PATTERN.get(regex.hashCode());
		if (pattern==null) {
			pattern = Pattern.compile(regex, flags);
			REGEX_PATTERN.put(regex.hashCode(), pattern);
		}
		return pattern;
	}

	/**
	 * 执行正则运算
	 *
	 * @param source 目标字符串
	 * @param regex  正则表达式
	 * @return Matcher对象
	 */
	public static Matcher doRegex(String source, String regex) {
		return doRegex(source, regex, 0);
	}

	/**
	 * 执行正则运算
	 *
	 * @param source 目标字符串
	 * @param regex  正则表达式
	 * @param flags  正则匹配标记 CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS
	 * @return Matcher对象
	 */
	public static Matcher doRegex(String source, String regex, Integer flags) {
		if (source == null) {
			return null;
		}

		Pattern pattern = getCachedPattern(regex, flags);
		Matcher matcher = pattern.matcher(source);
		if (matcher.find()) {
			return matcher;
		} else {
			return null;
		}
	}

	/**
	 * 正则表达式查找
	 * 匹配的被提取出来做数组
	 *
	 * @param source 目标字符串
	 * @param regex  正则表达式
	 * @return 匹配的字符串数组
	 */
	public static String[] searchByRegex(String source, String regex) {
		return searchByRegex(source, regex, 0);
	}

	/**
	 * 正则表达式查找
	 * 匹配的被提取出来做数组
	 *
	 * @param source 目标字符串
	 * @param regex  正则表达式
	 * @param flags  正则匹配标记 CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS
	 * @return 匹配的字符串数组
	 */
	public static String[] searchByRegex(String source, String regex, Integer flags) {
		if (source == null) {
			return null;
		}

		ArrayList<String> result = new ArrayList<String>();

		Matcher matcher = doRegex(source, regex, flags);

		if (matcher != null) {
			do {
				result.add(matcher.group());
			} while (matcher.find());
		}
		return result.toArray(new String[0]);
	}

	/**
	 * 正则匹配
	 *
	 * @param source 目标字符串
	 * @param regex  正则表达式
	 * @return 正则搜索后得到的匹配数量
	 */
	public static int regexMatch(String source, String regex) {
		return regexMatch(source, regex, 0);
	}

	/**
	 * 正则匹配
	 *
	 * @param source 目标字符串
	 * @param regex  正则表达式
	 * @param flags  正则匹配标记 CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS
	 * @return 正则搜索后得到的匹配数量
	 */
	public static int regexMatch(String source, String regex, Integer flags) {
		Matcher matcher = doRegex(source, regex, flags);

		int count = 0;
		if (matcher != null) {
			do {
				count++;
			} while (matcher.find());
		}
		return count;
	}

	/**
	 * 快速字符串替换算法
	 * 		默认正在 flag = 0, quoteReplacement = true
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @return 替换后的字符串
	 */
	public static String fastReplaceAll(String source, String regex, String replacement) {
		return fastReplaceAll(source, regex, replacement, 0, true);
	}

	/**
	 * 快速字符串替换算法
	 * 		默认正在 flag = 0, quoteReplacement = true
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @param flags  	  正则匹配标记 CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS
	 * @return 替换后的字符串
	 */
	public static String fastReplaceAll(String source, String regex, String replacement, int flags) {
		return fastReplaceAll(source, regex, replacement, flags, true);
	}

	/**
	 * 快速字符串替换算法
	 * 		默认正在 flag = 0
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @param quoteReplacement 对 replacement 是否进行转义
	 * @return 替换后的字符串
	 */
	public static String fastReplaceAll(String source, String regex, String replacement, boolean quoteReplacement) {
		return fastReplaceAll(source, regex, replacement, 0, quoteReplacement);
	}

	/**
	 * 快速字符串替换算法
	 *
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @param flags  	  正则匹配标记 CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS
	 * @param quoteReplacement 对 replacement 是否进行转义
	 * @return 替换后的字符串
	 */
	public static String fastReplaceAll(String source, String regex, String replacement, Integer flags, boolean quoteReplacement) {
		Pattern pattern = getCachedPattern(regex, flags);
		if(quoteReplacement) {
			replacement = Matcher.quoteReplacement(replacement);
		}
		return pattern.matcher(source).replaceAll(replacement);
	}


	/**
	 * 快速字符串替换算法
	 * 		默认正在 flag = 0, quoteReplacement = true
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @return 替换后的字符串
	 */
	public static String fastReplaceFirst(String source, String regex, String replacement) {
		return fastReplaceFirst(source, regex, replacement, 0, true);
	}

	/**
	 * 快速字符串替换算法
	 * 		默认正在 flag = 0, quoteReplacement = true
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @param flags  	  正则匹配标记 CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS
	 * @return 替换后的字符串
	 */
	public static String fastReplaceFirst(String source, String regex, String replacement, int flags) {
		return fastReplaceFirst(source, regex, replacement, flags, true);
	}

	/**
	 * 快速字符串替换算法
	 * 		默认正在 flag = 0
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @param quoteReplacement 对 replacement 是否进行转义
	 * @return 替换后的字符串
	 */
	public static String fastReplaceFirst(String source, String regex, String replacement, boolean quoteReplacement) {
		return fastReplaceFirst(source, regex, replacement, 0, quoteReplacement);
	}

	/**
	 * 快速字符串替换算法
	 *
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @param flags  	  正则匹配标记 CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, UNIX_LINES, LITERAL, UNICODE_CHARACTER_CLASS, COMMENTS
	 * @param quoteReplacement 对 replacement 是否进行转义
	 * @return 替换后的字符串
	 */
	public static String fastReplaceFirst(String source, String regex, String replacement, Integer flags, boolean quoteReplacement) {
		Pattern pattern = getCachedPattern(regex, flags);
		if(quoteReplacement) {
			replacement = Matcher.quoteReplacement(replacement);
		}
		return pattern.matcher(source).replaceFirst(replacement);
	}

	/**
	 * 判断字符串空指针或者内容为空
	 *
	 * @param source 字符串
	 * @return 是否是空指针或者内容为空
	 */
	public static boolean isNullOrEmpty(String source) {
		if (source == null || source.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	private static char SICHAR = (char)26;
	/**
	 * 按照标识符 Map 进行替换
	 *
	 * @param source 源字符串,标识符使用"{标识}"进行包裹,这些标识符将会被替换
	 * @param tokens 标识符 Map, 包含匹配字符串和替换字符串
	 * @return 替换后的字符串
	 */
	public static String tokenReplace(String source, Map<String, ?> tokens) {
		if (source == null) {
			return null;
		}

		for (Entry<String, ?> entry : tokens.entrySet()) {
			String value = entry.getValue() == null ? "null" : entry.getValue().toString();
			//value 中的 TOKEN 符号转换
			value = value.replaceAll(TOKEN_PREFIX_REGEX, TOKEN_PREFIX+SICHAR);
			value = value.replaceAll(TOKEN_SUFFIX_REGEX, SICHAR+TOKEN_SUFFIX);
			source = oneTokenReplace(source, entry.getKey(), value);
		}

		//value 中的 TOKEN 符号反向转换
		source = source.replaceAll(TOKEN_PREFIX_REGEX+SICHAR, TOKEN_PREFIX);
		source = source.replaceAll(SICHAR + TOKEN_SUFFIX_REGEX, TOKEN_SUFFIX);
		return source;
	}

	/**
	 * 按照标识符 Map 进行替换
	 *
	 * @param source 源字符串,标识符使用"{标识}"进行包裹,这些标识符将会被替换
	 * @param list   数据 List 集合
	 * @return 替换后的字符串
	 */
	public static String tokenReplace(String source, List<Object> list) {

		Map<String, Object> tokens = TObject.arrayToMap(list.toArray());
		return tokenReplace(source, tokens);
	}

	/**
	 * 按位置格式化字符串
	 * TString.tokenReplace("aaaa{1}bbbb{2}cccc{3}", "1","2","3")
	 * 或者TString.tokenReplace("aaaa{}bbbb{}cccc{}", "1","2","3")
	 * 输出aaaa1bbbb2cccc3
	 *
	 * @param source 字符串
	 * @param args   多个参数
	 * @return 格式化后的字符串
	 */
	public static String tokenReplace(String source, Object... args) {
		if (source == null) {
			return null;
		}

		source = tokenReplace(source, TObject.arrayToMap(args));
		return source;
	}

	public static String TOKEN_PREFIX = "{";
	public static String TOKEN_SUFFIX = "}";
	public static String TOKEN_EMPTY = TOKEN_PREFIX + TOKEN_SUFFIX;

	public static String TOKEN_PREFIX_REGEX = "\\" + TOKEN_PREFIX;
	public static String TOKEN_SUFFIX_REGEX = "\\" + TOKEN_SUFFIX;
	public static String TOKEN_EMPTY_REGEX  = TOKEN_PREFIX_REGEX + TOKEN_SUFFIX_REGEX;

	public static String wrapToken(String token) {
		return TString.TOKEN_PREFIX + token + TString.TOKEN_SUFFIX;
	}

	/**
	 * 按照标识符进行替换
	 * tokenName 为 null 则使用 {} 进行替换
	 * 如果为 tokenName 为数字 可以不在标识中填写数字标识,会自动按照标识的先后顺序进行替换
	 *
	 * @param source     源字符串,标识符使用"{标识}"进行包裹
	 * @param tokenName  标识符
	 * @param tokenValue 标志符值
	 * @return 替换后的字符串
	 */
	public static String oneTokenReplace(String source, String tokenName, String tokenValue) {
		if (source == null) {
			return null;
		}

		if (source.contains(TOKEN_PREFIX + tokenName + TOKEN_SUFFIX)) {
			return fastReplaceAll(source, TOKEN_PREFIX_REGEX + tokenName + TOKEN_SUFFIX_REGEX,
					tokenValue == null ? "null" : Matcher.quoteReplacement(tokenValue));
		} else if ((tokenName == null || TString.isInteger(tokenName)) &&
				source.contains(TOKEN_EMPTY)) {
			return fastReplaceFirst(source, TOKEN_EMPTY_REGEX, tokenValue);
		} else {
			return source;
		}
	}

	/**
	 * 替换第一个标志字符串
	 *
	 * @param source      字符串
	 * @param mark        标志字符
	 * @param replacement 替换字符
	 * @return 替换后的结果
	 */
	public static String replaceFirst(String source, String mark, String replacement) {
		if (source == null) {
			return null;
		}

		int head = source.indexOf(mark);
		int tail = head + mark.length();
		replacement = TObject.nullDefault(replacement, "");
		source = source.substring(0, head) + replacement + source.substring(tail, source.length());
		return source;
	}

	/**
	 * 替换最后一个标志字符串
	 *
	 * @param source      字符串
	 * @param mark        标志字符
	 * @param replacement 替换字符
	 * @return 替换后的结果
	 */
	public static String replaceLast(String source, String mark, String replacement) {
		if (source == null) {
			return null;
		}
		int head = source.lastIndexOf(mark);
		int tail = head + mark.length();
		replacement = TObject.nullDefault(replacement, "");
		source = source.substring(0, head) + replacement + source.substring(tail, source.length());
		return source;
	}

	/**
	 * 缩进字符串
	 *
	 * @param source      待缩进的字符串
	 * @param indentCount 缩进数(空格的数目)
	 * @return 缩进后的字符串
	 */
	public static String indent(String source, int indentCount) {
		if (indentCount > 0 && source != null) {
			StringBuilder indent = new StringBuilder();
			for (int i = 0; i < indentCount; i++) {
				indent.append(" ");
			}
			source = indent + source;
			source = fastReplaceAll(source, "\n", "\n" + indent);
		}
		return source;
	}

	/**
	 * 翻转字符串 输入1234 输出4321
	 *
	 * @param source 字符串
	 * @return 翻转后的字符串
	 */
	public static String reverse(String source) {
		if (source != null) {
			char[] array = source.toCharArray();
			StringBuilder reverse = new StringBuilder();
			for (int i = array.length - 1; i >= 0; i--)
				reverse.append(array[i]);
			return reverse.toString();
		}
		return null;
	}

	/**
	 * 将系统转义字符,转义成可在字符串表达的转义字符
	 * 例如:将字符串中的 \" 转转成 \\\"
	 *
	 * @param source 源字符串
	 * @return 转换后的字符串
	 */
	public static String convertEscapeChar(String source) {
		if (source == null) {
			return null;
		}

		source = fastReplaceAll(source, "\\[^u][^[0-9|a-f]]{4}", "\\u005c");
		source = fastReplaceAll(source, "\f", "\\u000c");
		source = fastReplaceAll(source, "\'", "\\u0027");
		source = fastReplaceAll(source, "\r", "\\u000d");
		source = fastReplaceAll(source, "\"", "\\u0022");
		source = fastReplaceAll(source, "\b", "\\u0008");
		source = fastReplaceAll(source, "\t", "\\u0009");
		source = fastReplaceAll(source, "\n", "\\u000a");
		return source;
	}

	/**
	 * 将可在字符串中表达的转义字符,转义成系统转义字符
	 * 例如:将字符串中的 \\\" 转转成 \"
	 *
	 * @param source 源字符串
	 * @return 转换后的字符串
	 */
	public static String unConvertEscapeChar(String source) {
		if (source == null) {
			return null;
		}

		source = fastReplaceAll(source, "\\\\u005c", "\\");
		source = fastReplaceAll(source, "\\\\u000c", "\f");
		source = fastReplaceAll(source, "\\\\u0027", "\'");
		source = fastReplaceAll(source, "\\\\u000d", "\r");
		source = fastReplaceAll(source, "\\\\u0022", "\"");
		source = fastReplaceAll(source, "\\\\u0008", "\b");
		source = fastReplaceAll(source, "\\\\u0009", "\t");
		source = fastReplaceAll(source, "\\\\u000a", "\n");
		return source;
	}


	/**
	 * 字符串转 Unicode
	 *
	 * @param source 字符串
	 * @return unicode 字符串
	 */
	public static String toUnicode(String source) {

		if (source == null) {
			return null;
		}

		StringBuffer result = new StringBuffer();

		for (int i = 0; i < source.length(); i++) {

			// 取出一个字符
			char c = source.charAt(i);

			// 转换为unicode
			result.append("\\u" + leftPad(Integer.toHexString(c), 4, '0'));
		}

		return result.toString();
	}

	/**
	 * Unicode 转 字符串
	 *
	 * @param source unicode 字符串
	 * @return string 字符串
	 */
	public static String fromUnicode(String source) {
		if (source == null) {
			return null;
		}

		if (source.contains("\\u")) {

			StringBuffer result = new StringBuffer();

			String[] hex = source.split("\\\\u");

			for (int i = 0; i < hex.length; i++) {
				String element = hex[i];
				if (element.length() >= 4) {
					String codePoint = element.substring(0, 4);
					// 转换码点
					int charCode = Integer.parseInt(codePoint, 16);
					result.append((char) charCode);
					element = element.substring(4, element.length());
				}
				// 追加成string
				result.append(element);
			}
			return result.toString();
		} else {
			return source;
		}

	}

	/**
	 * 字符串转换为 Java 基本类型
	 *
	 * @param value      字符串字面值
	 * @param type       Type类型
	 * @param ignoreCase 是否在字段匹配时忽略大小写
	 * @param <T>        范型
	 * @return 基本类型对象
	 */
	public static <T> T toObject(String value, Type type, boolean ignoreCase) {
		if(value==null || type==null) {
			return null;
		}
		GenericInfo genericInfo = TReflect.getGenericInfo(type);
		Class<?> clazz = genericInfo.getClazz();

		if (value == null && !clazz.isPrimitive()) {
			return null;
		}

		if(value.equals("null") && !clazz.isPrimitive()){
			return null;
		}

		if (clazz == int.class || clazz == Integer.class) {
			value = value == null ? "0" : value;
			return (T) Integer.valueOf(value.trim());
		} else if (clazz == float.class || clazz == Float.class) {
			value = value == null ? "0" : value;
			return (T) Float.valueOf(value.trim());
		} else if (clazz == double.class || clazz == Double.class) {
			value = value == null ? "0" : value;
			return (T) Double.valueOf(value.trim());
		} else if (clazz == boolean.class || clazz == Boolean.class) {
			if(value == null || value.equals("0")) {
				value = "false";
			} else if(value.equals("1")){
				value = "true";
			}
			return (T) Boolean.valueOf(value.trim());
		} else if (clazz == long.class || clazz == Long.class) {
			value = value == null ? "0" : value;
			return (T) Long.valueOf(value.trim());
		} else if (clazz == short.class || clazz == Short.class) {
			value = value == null ? "0" : value;
			return (T) Short.valueOf(value.trim());
		} else if (clazz == byte.class || clazz == Byte.class) {
			value = value == null ? "0" : value;
			return (T) Byte.valueOf(value.trim());
		} else if (clazz == char.class || clazz == Character.class) {
			Object tmpValue = value != null ? value.charAt(0) : null;
			return (T) tmpValue;
		} else if (clazz == Date.class || TReflect.isExtends(clazz,  Date.class)) {
			try {
				SimpleDateFormat dateFormat = new SimpleDateFormat(TDateTime.STANDER_DATETIME_TEMPLATE);
				return (T) (value != null ? TReflect.newInstance(clazz, dateFormat.parse(value).getTime()): null);
			} catch (Exception e){
				Logger.error("TString.toObject error: ", e);
				return null;
			}
		} else if ((TReflect.isImp(clazz, Collection.class) || clazz.isArray()) &&
				JSON.isJSONList(value)) {
			return JSON.toObject(value, type, ignoreCase);
		} else if (TReflect.isImp(clazz, Map.class) && JSON.isJSONMap(value)) {
			return JSON.toObject(value, type, ignoreCase);
		} else if (JSON.isJSON(value) && !TReflect.isSystemType(clazz)) {
			return JSON.toObject(value, type, ignoreCase);
		} else if (value.startsWith("\"") && value.endsWith("\"")) {
			return (T) value.substring(1, value.length() - 1);
		} else if(clazz == BigDecimal.class){
			return (T)new BigDecimal(value.trim());
		} else {
			return (T) value;
		}
	}

	/**
	 * 字符串转换为 Java 基本类型
	 *
	 * @param value 字符串字面值
	 * @param type  Type类型
	 * @param <T>   范型
	 * @return 基本类型对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T toObject(String value, Type type) {
		return (T) toObject(value, type, false);
	}

	public static char[] chars = new char[]{'0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
			'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V',
			'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
			'w', 'x', 'y', 'z'};

	public static Map<Character, Integer> unChars = new HashMap<Character, Integer>();

	static {
		initUnchars();
	}

	public static void setChars(char[] argChars) {
		argChars = chars;
		initUnchars();
	}

	private static void initUnchars() {
		for(int i=0;i<chars.length;i++) {
			unChars.put(chars[i],i);
		}
	}

	public static String radixConvert(long num, int radix) {

		if (radix < 2 || radix > 62) {
			return null;
		}

		num = num < 0 ? num*-1 : num;

		String result = "";

		long tmpValue = num;

		while (true) {
			long value = (int) (tmpValue % radix);
			result = chars[(int) value] + result;
			value = tmpValue / radix;
			if (value >= radix) {
				tmpValue = value;
			} else {
				result = chars[(int) value] + result;
				break;
			}
		}

		return result;
	}

	public static long radixUnConvert(String str, int radix) {
		long result = unChars.get(str.charAt(0));
		for(int i=1; i<str.length();i++){
			char c = str.charAt(i);
			int val = unChars.get(c);
			result = result * radix + val;
		}

		return result;

	}

	public static String radixBigConvert(BigDecimal n_num, int n_radix) {
		if (n_radix < 2 || n_radix > 62) {
			return null;
		}

		BigDecimal num = n_num;
		BigDecimal radix = BigDecimal.valueOf(n_radix);

		num = num.compareTo(BigDecimal.ZERO) < 0 ? num.multiply(BigDecimal.valueOf(-1)) : num;

		String result = "";

		BigDecimal tmpValue = num;

		while (true) {
			BigDecimal[] drValue = tmpValue.divideAndRemainder(radix);
			BigDecimal value = drValue[1];
			result = chars[value.intValue()] + result;
			value = drValue[0];
			if (value.compareTo(radix)>=0) {
				tmpValue = value;
			} else {
				result = chars[value.intValue()] + result;
				break;
			}
		}

		return result;
	}

	public static BigDecimal radixBigUnConvert(String str, int n_radix) {
		BigDecimal radix = BigDecimal.valueOf(n_radix);

		BigDecimal result = BigDecimal.valueOf(unChars.get(str.charAt(0)));
		for(int i=1; i<str.length();i++){
			char c = str.charAt(i);
			int val = unChars.get(c);
			result = result.multiply(radix).add(BigDecimal.valueOf(val));
		}

		return result;

	}


	/**
	 * 生成短 UUID
	 *
	 * @return 生成的短 UUID
	 */
	public static String generateShortUUID() {
		StringBuffer shortBuffer = new StringBuffer();
		String uuid = UUID.randomUUID().toString();
		uuid = uuid.replace("-", "");
		for (int i = 0; i < 8; i++) {
			String str = uuid.substring(i * 4, i * 4 + 4);
			int x = Integer.parseInt(str, 16);
			shortBuffer.append(radixConvert(x, 62));
		}
		return shortBuffer.toString();

	}

	/**
	 * 快速生成短ID
	 * @return 快速生成短的ID
	 */
	public static String generateId() {
		return generateId(null, null);
	}

	/**
	 * 快速生成短ID
	 * @param obj 生成 id 的对象
	 * @return 快速生成短的ID
	 */
	public static String generateId(Object obj) {
		return generateId(obj, null);
	}

	/**
	 * 快速生成短ID
	 * @param obj 生成 id 的对象
	 * @param sign 生成 ID 的标记
	 * @return 快速生成短的ID
	 */
	public static String generateId(Object obj, String sign) {
		ThreadLocalRandom random = ThreadLocalRandom.current();

		if(sign == null){
			sign = Global.FRAMEWORK_NAME;
		}

		if(obj==null) {
			obj = random.nextInt();
		}

		long currentTime = TDateTime.currentTimeNanos();
		long mark = currentTime ^ random.nextLong();

		long randomMark = currentTime ^random.nextLong();

		long id = obj.hashCode()^Runtime.getRuntime().freeMemory()^mark^(new Random(randomMark).nextLong())^Long.valueOf(sign, 36);
		return radixConvert(id, 62);
	}



	/**
	 * 获取字符串中最长一行的长度
	 *
	 * @param source unicode 字符串
	 * @return 最长一行的长度
	 */
	public static int maxLineLength(String source) {
		String[] lines = source.split("\n");

		int maxLineLength = -1;

		for (String line : lines) {
			if (maxLineLength < line.length()) {
				maxLineLength = line.length();
			}
		}

		return maxLineLength;
	}

	/**
	 * 根据分割符把字符串分割成一个数组
	 *
	 * @param source 源字符串
	 * @param regex  正则分割符
	 * @return 字符串数组
	 */
	public static String[] split(String source, String regex) {
		if (source == null) {
			return null;
		}

		StringTokenizer stringTokenizer = new StringTokenizer(source, regex);
		ArrayList<String> items = new ArrayList<String>();

		while (stringTokenizer.hasMoreTokens()){
			items.add(stringTokenizer.nextToken());
		}


		return items.toArray(new String[items.size()]);
	}

	/**
	 * 在字符串中插入字符
	 * @param source    源字符串
	 * @param position  插入位置, 从 1 开始
	 * @param value     插入的字符串
	 * @return  插入后的字符串数据
	 */
	public static String insert(String source, int position, String value){
		if(position <=0 ){
			throw new IllegalArgumentException("parameter position must lager than 0");
		}
		return source.substring(0, position) + value + source.substring(position);
	}

	/**
	 * 移除字符串尾部的换行符
	 * @param source 源字符串
	 * @return 处理后的字符串
	 */
	public static String trimEndLF(String source){
		return source.replaceAll("[\r\n]*$", "");
	}


	/**
	 * 获取字符串的缩进
	 * @param source 源字符串
	 * @return 缩进
	 */
	public static int retract(String source){
		int currentRetract;
		for (currentRetract = 0; currentRetract < source.length(); currentRetract++) {
			if (source.charAt(currentRetract) != ' ') {
				if (currentRetract > 0) {
					currentRetract--;
				}
				break;
			}
		}

		return currentRetract;
	}

	/**
	 * 驼峰命名转下划线命名
	 * @param param 驼峰命名字符串
	 * @return 下划线命名字符串
	 */
	public static String camelToUnderline(String param){
		if (param == null||"".equals(param.trim())){
			return "";
		}
		int len = param.length();
		StringBuilder sb = new StringBuilder(len);
		sb.append(param.charAt(0));
		for (int i = 1; i < len; i++) {
			char c=param.charAt(i);
			if (Character.isUpperCase(c)){
				sb.append("_");
				sb.append(Character.toLowerCase(c));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 下划线命名转驼峰命名
	 * @param param 下划线命名字符串
	 * @return 驼峰命名字符串
	 */
	public static String underlineToCamel(String param){
		if (param == null||"".equals(param.trim())){
			return "";
		}
		int len=param.length();
		StringBuilder sb=new StringBuilder(len);
		sb.append(param.charAt(0));
		for (int i = 1; i < len; i++) {
			char c = param.charAt(i);
			if (c == '_'){
				if (++i<len){
					sb.append(Character.toUpperCase(param.charAt(i)));
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 字符串快捷拼装方法
	 * @param items 需要拼装的字符串
	 * @return 拼装后的字符串
	 */
	public static String assembly(Object ... items){
		StringBuilder stringBuilder = new StringBuilder();
		for(Object item : items){
			stringBuilder.append(item);
		}

		return stringBuilder.toString();
	}

	/**
	 * 字符串快捷拼装方法
	 * @param splitor 分割字符串
	 * @param items 需要拼装的字符串
	 * @return 拼装后的字符串
	 */
	public static String join(String splitor,  Object ... items){
		StringBuilder stringBuilder = new StringBuilder();
		for(Object item : items){
			stringBuilder.append(item + splitor);
		}

		stringBuilder.substring(0, stringBuilder.length()-splitor.length());

		return stringBuilder.toString();
	}

	/**
	 * 将字符串转化成 Ascii 的 byte[]
	 * @param str 需要转换的字符串
	 * @return 转换后的字节队列
	 */
	public static byte[] toAsciiBytes(String str) {
		byte[] bytes = new byte[str.length()];
		for(int i=0;i<str.length();i++){
			bytes[i] = (byte)str.charAt(i);
		}

		return bytes;
	}

	/**
	 * byte 转字符串
	 * @param bytes  字节数据
	 * @param offset 字节数据偏移量
	 * @param length 长度
	 * @return 转换后的字符串
	 */
	public static String toAsciiString(byte[] bytes, int offset, int length) {
		StringBuilder stringBuilder = new StringBuilder(length);
		for(int i=offset;i<length;i++){
			stringBuilder.append((char)(bytes[i] & 0xFF));
		}

		return stringBuilder.toString();
	}

	/**
	 * 从指定位置读取一行字符串
	 * @param str 读取的字符串
	 * @param beginIndex 起始位置
	 * @return 读取的一行字符串, null 读取到结尾
	 */
	public static String readLine(String str, int beginIndex) {
		if(beginIndex==str.length()){
			return null;
		}

		int lineIndex = str.indexOf("\n", beginIndex);
		lineIndex = lineIndex == -1 ? str.length():lineIndex + 1;
		return str.substring(beginIndex, lineIndex);
	}

	private static String formatUnits(long value, long prefix, String unit) {
		if (value % prefix == 0) {
			return String.format("%d %s", value / prefix, unit);
		}
		return String.format("%.1f %s", (double) value / prefix, unit);
	}

	private static final long KIBI = 1L << 10;
	private static final long MEBI = 1L << 20;
	private static final long GIBI = 1L << 30;
	private static final long TEBI = 1L << 40;
	private static final long PEBI = 1L << 50;
	private static final long EXBI = 1L << 60;

	/**
	 * 格式化计算机字节单位的存储容量为文本
	 * @param byteCount 字节数
	 * @return 格式化后的字符串
	 */
	public static String formatBytes(long byteCount) {
		if (byteCount == 1L) { // bytes
			return String.format("%d byte", byteCount);
		} else if (byteCount < KIBI) { // bytes
			return String.format("%d bytes", byteCount);
		} else if (byteCount < MEBI) { // KiB
			return formatUnits(byteCount, KIBI, "KB");
		} else if (byteCount < GIBI) { // MiB
			return formatUnits(byteCount, MEBI, "MB");
		} else if (byteCount < TEBI) { // GiB
			return formatUnits(byteCount, GIBI, "GB");
		} else if (byteCount < PEBI) { // TiB
			return formatUnits(byteCount, TEBI, "TB");
		} else if (byteCount < EXBI) { // PiB
			return formatUnits(byteCount, PEBI, "PB");
		} else { // EiB
			return formatUnits(byteCount, EXBI, "EB");
		}
	}

	private static final long KILO = 1000L;
	private static final long MEGA = 1000000L;
	private static final long GIGA = 1000000000L;
	private static final long TERA = 1000000000000L;
	private static final long PETA = 1000000000000000L;
	private static final long EXA = 1000000000000000000L;

	/**
	 * 按数学技术格式化数字为文本
	 * @param value 数量
	 * @param unit 单位(K,M,G,T,P,E)
	 * @return 格式化后的字符串
	 */
	public static String formatNumber(long value, String unit) {
		if (value < KILO) {
			return String.format("%d %s", value, unit);
		} else if (value < MEGA) { // K
			return formatUnits(value, KILO, "K" + unit);
		} else if (value < GIGA) { // M
			return formatUnits(value, MEGA, "M" + unit);
		} else if (value < TERA) { // G
			return formatUnits(value, GIGA, "G" + unit);
		} else if (value < PETA) { // T
			return formatUnits(value, TERA, "T" + unit);
		} else if (value < EXA) { // P
			return formatUnits(value, PETA, "P" + unit);
		} else { // E
			return formatUnits(value, EXA, "E" + unit);
		}
	}

	/**
	 * 从指定的 URL 读取内容
	 * @param endPoint url的短点字符串
	 * @param timeout  超时时间
	 * @return 读取的内容
	 * @throws IOException 异常
	 */
	public static String loadURL(String endPoint, int timeout) throws IOException {
		URL url = new URL(endPoint);
		URLConnection urlConnection =  url.openConnection();
		urlConnection.setConnectTimeout(timeout);
		urlConnection.connect();
		InputStream stream = (InputStream)urlConnection.getContent();
		return new String(TStream.readAll(stream));
	}

	/**
	 * 从指定的 URL 读取内容
	 * 		默认 5s 超时
	 * @param endPoint url的短点字符串
	 * @return 读取的内容
	 * @throws IOException 异常
	 */
	public static String loadURL(String endPoint) throws IOException {
		return loadURL(endPoint, 5000);
	}

	public static String NUMBERS = "0123456789";
	public static String CHAR_LOW_CASE = "abcdefghijklmnopqrstuvwxyz";
	public static String CHAR_UPPER_CASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static String SPECIAL_CHAR = "~!@#$%^&*()[{]}-_=+|;:'\",<.>/?`";

	/**
	 * 生成随机字符串
	 * @param length 随机字符串长度
	 * @param useNumber 是否使用数字
	 * @param useLowcaseChar 是否使用小写字母
	 * @param useUppercaseChar 是否使用大写字母
	 * @param useSpecialChar 是否使用特殊字符
	 * @return 生成随机字符串
	 */
	public static String genRandomString(int length, boolean useNumber, boolean useLowcaseChar, boolean useUppercaseChar, boolean useSpecialChar){
		String allChar = (useNumber? NUMBERS : "") +  (useLowcaseChar? CHAR_LOW_CASE : "") + 
						(useUppercaseChar? CHAR_UPPER_CASE : "") +  (useSpecialChar? SPECIAL_CHAR : "");

		StringBuilder stringBuilder = new StringBuilder();
		Random random = new Random();
		for(int i=0;i<length;i++) {
			int index = random.nextInt(allChar.length()-1);
			stringBuilder.append(allChar.charAt(index));
		}

		return stringBuilder.toString();
	}

	public static String genRandomString(int length){
		return genRandomString(length, true, true, true, false);
	}

	/**
	 * 按 step 将随机字符串织入目标字符串
	 * @param str 目标字符串
	 * @param step 步长
	 * @return 织入后的字符串
	 */
	public static String weave(String str, int step) {
        String randomString = TString.genRandomString(str.length()*step);

        StringBuilder builder = new StringBuilder();
        for(int i=0;i<str.length();i++) {
			for(int p=0;p<step;p++) {
            	builder.append(randomString.charAt(i*step + p));
			}
            builder.append(str.charAt(i));
        }
        return builder.toString();
    }

	/**
	 * 解析织入的字符串,获得原始字符串
	 * @param str 织入后的字符串
	 * @param step 步长
	 * @return 原始字符串
	 */
    public static String unWeave(String str, int step) {
        StringBuilder builder = new StringBuilder();
        for(int i=step;i<str.length();i=i+step+1) {
            builder.append(str.charAt(i));
        } 
        return builder.toString();
    }


	/**
	 * 获取 Url 字符串中的服务器地址
	 * 		例如: http://127.0.0.1:8080/userinfo -> http://127.0.0.1:8080
	 * @param url
	 * @return
	 */
	public static String getUrlHost(String url) {
        int index = url.indexOf('/', 8);
		if(index<0) {
			return url;
		}
        return url.substring(0, index);
    }

	/**
	* 获取 Url 字符串中的路由
	* 		例如: http://127.0.0.1:8080/userinfo -> /userinfo
	* @param url
	* @return
	*/
    public static String getUrlPath(String url) {
        int index = url.indexOf('/', 8);
		if(index<0) {
			return "";
		}
        return url.substring(index, url.length());
    }


}
