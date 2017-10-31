package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String 工具类
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TString {

	private static Map<Integer, Pattern> regexPattern = new ConcurrentHashMap<Integer, Pattern>();

	/**
	 * 单词首字母大写
	 *
	 * @param source 字符串
	 * @return 首字母大写后的字符串
	 */
	public static String uppercaseHead(String source) {
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
	 * @param floadString 浮点数字符串
	 * @return 是否是浮点数
	 */
	public static boolean isFloat(String floadString) {
		if (floadString != null && regexMatch(floadString, "^-?\\d+\\.\\d+$") > 0) {
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


	private static Pattern getCachedPattern(String regex) {
		Pattern pattern = null;
		if (regexPattern.containsKey(regex.hashCode())) {
			pattern = regexPattern.get(regex.hashCode());
		} else {
			pattern = Pattern.compile(regex);
			regexPattern.put(regex.hashCode(), pattern);
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
		if (source == null) {
			return null;
		}

		Pattern pattern = getCachedPattern(regex);
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
		if (source == null) {
			return null;
		}

		ArrayList<String> result = new ArrayList<String>();

		Matcher matcher = doRegex(source, regex);
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
		return searchByRegex(source, regex).length;
	}

	/**
	 * 快速字符串替换算法
	 *
	 * @param source      源字符串
	 * @param regex       正则字符串
	 * @param replacement 替换字符串
	 * @return 替换后的字符串
	 */
	public static String fastReplaceAll(String source, String regex, String replacement) {
		Pattern pattern = getCachedPattern(regex);
		return pattern.matcher(source).replaceAll(Matcher.quoteReplacement(replacement));
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

	/**
	 * 按照标识符 Map 进行替换
	 *
	 * @param source 源字符串,标识符使用"{{标识}}"进行包裹,这些标识符将会被替换
	 * @param tokens 标识符Map集合
	 * @return 替换后的字符串
	 */
	public static String tokenReplace(String source, Map<String, ?> tokens) {
		if (source == null) {
			return null;
		}

		for (Entry<String, ?> entry : tokens.entrySet()) {
			String value = entry.getValue() == null ? "null" : entry.getValue().toString();
			source = oneTokenReplace(source, entry.getKey(), entry.getValue().toString());
		}
		return source;
	}

	/**
	 * 按照标识符 Map 进行替换
	 *
	 * @param source 源字符串,标识符使用"{{标识}}"进行包裹,这些标识符将会被替换
	 * @param list   数据 List 集合
	 * @return 替换后的字符串
	 */
	public static String tokenReplace(String source, List<Object> list) {

		Map<String, Object> tokens = TObject.arrayToMap(list.toArray());
		return tokenReplace(source, tokens);
	}

	/**
	 * 按位置格式化字符串
	 * TString.tokenReplace("aaaa{{1}}bbbb{{2}}cccc{{3}}", "1","2","3")
	 * 或者TString.tokenReplace("aaaa{{}}bbbb{{}}cccc{{}}", "1","2","3")
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

	public static String TOKEN_PREFIX_REGEX = "\\{\\{";
	public static String TOKEN_SUFFIX_REGEX = "\\}\\}";


	/**
	 * 按照标识符进行替换
	 * tokenName 为 null 则使用 {{}} 进行替换
	 * 如果为 tokenName 为数字 可以不在标识中填写数字标识,会自动按照标识的先后顺序进行替换
	 *
	 * @param source     源字符串,标识符使用"{{标识}}"进行包裹
	 * @param tokenName  标识符
	 * @param tokenValue 标志符值
	 * @return 替换后的字符串
	 */
	public static String oneTokenReplace(String source, String tokenName, String tokenValue) {
		String TOKEN_PREFIX = TString.fastReplaceAll(TOKEN_PREFIX_REGEX, "\\\\", "");
		String TOKEN_SUFFIX = TString.fastReplaceAll(TOKEN_SUFFIX_REGEX, "\\\\", "");
		String TOKEN_EMPTY = TOKEN_PREFIX + TOKEN_SUFFIX;

		if (source == null) {
			return null;
		}

		if (source.contains(TOKEN_PREFIX + tokenName + TOKEN_SUFFIX)) {
			return fastReplaceAll(source, TOKEN_PREFIX_REGEX + tokenName + TOKEN_SUFFIX_REGEX,
					tokenValue == null ? "null" : Matcher.quoteReplacement(tokenValue));
		} else if ((tokenName == null || TString.isInteger(tokenName)) &&
				source.contains(TOKEN_EMPTY)) {
			return TString.replaceFirst(source, TOKEN_EMPTY, tokenValue);
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
			source = indent.toString() + source;
			source = TString.fastReplaceAll(source, "\n", "\n" + indent.toString());
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

		source = fastReplaceAll(source, "\\u005c", "\\");
		source = fastReplaceAll(source, "\\u000c", "\f");
		source = fastReplaceAll(source, "\\u0027", "\'");
		source = fastReplaceAll(source, "\\u000d", "\r");
		source = fastReplaceAll(source, "\\u0022", "\"");
		source = fastReplaceAll(source, "\\u0008", "\b");
		source = fastReplaceAll(source, "\\u0009", "\t");
		source = fastReplaceAll(source, "\\u000a", "\n");
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

		Class<?> clazz = null;
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			clazz = (Class<T>) parameterizedType.getRawType();
		} else if (type instanceof Class) {
			clazz = (Class<T>) type;
		} else {
			return (T) value;
		}

		if (value == null && !clazz.isPrimitive()) {
			return null;
		} else if (clazz == int.class || clazz == Integer.class) {
			value = value == null ? "0" : value;
			return (T) Integer.valueOf(value);
		} else if (clazz == float.class || clazz == Float.class) {
			value = value == null ? "0" : value;
			return (T) Float.valueOf(value);
		} else if (clazz == double.class || clazz == Double.class) {
			value = value == null ? "0" : value;
			return (T) Double.valueOf(value);
		} else if (clazz == boolean.class || clazz == Boolean.class) {
			value = value == null ? "false" : value;
			return (T) Boolean.valueOf(value);
		} else if (clazz == long.class || clazz == Long.class) {
			value = value == null ? "0" : value;
			return (T) Long.valueOf(value);
		} else if (clazz == short.class || clazz == Short.class) {
			value = value == null ? "0" : value;
			return (T) Short.valueOf(value);
		} else if (clazz == byte.class || clazz == Byte.class) {
			value = value == null ? "0" : value;
			return (T) Byte.valueOf(value);
		} else if (clazz == char.class || clazz == Character.class) {
			Object tmpValue = value != null ? value.charAt(0) : null;
			return (T) tmpValue;
		} else if ((TReflect.isImpByInterface(clazz, Collection.class) || clazz.isArray()) &&
				JSON.isJSONList(value)) {
			return JSON.toObject(value, type, ignoreCase);
		} else if (TReflect.isImpByInterface(clazz, Map.class) && JSON.isJSONMap(value)) {
			return JSON.toObject(value, type, ignoreCase);
		} else if (JSON.isJSON(value) && !TReflect.isSystemType(clazz)) {
			return JSON.toObject(value, type, ignoreCase);
		} else if (value.startsWith("\"") && value.endsWith("\"")) {
			return (T) value.substring(1, value.length() - 1);
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

	public static String[] chars = new String[]{"0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
			"J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
			"W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f", "g", "h", "i",
			"j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
			"w", "x", "y", "z"};

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
	 * @sign 生成 ID 的标记
	 * @return 快速生成短的ID
	 */
	public static String generateId(Object obj) {
		return generateId(obj, null);
	}

	/**
	 * 快速生成短ID
	 * @param obj 生成 id 的对象
	 * @return 快速生成短的ID
	 */
	public static String generateId(Object obj, String sign) {
		Random random = new Random();

		if(sign == null){
			sign = Global.NAME;
		}

		if(obj==null){
			obj = random.nextInt();
		}

		long currentTime = System.currentTimeMillis();
		long mark = currentTime ^ random.nextLong();

		random.setSeed(mark);
		long randomMark = currentTime ^random.nextLong();

		long id = obj.hashCode()^Runtime.getRuntime().freeMemory()^mark^(new Random(randomMark).nextLong())^Long.valueOf(sign, 36);
		return TString.radixConvert(id, 62);
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

		ArrayList<String> items = new ArrayList<String>();
		for (int position = source.indexOf(regex);
		     position > 0;
		     position = source.indexOf(regex)) {
			items.add(source.substring(0, position));
			source = source.substring(position + 1, source.length());
		}

		if (!source.isEmpty()) {
			items.add(source);
		}

		return items.toArray(new String[0]);
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
		return source.substring(0, position-1) + value + source.substring(position-1);
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
}
