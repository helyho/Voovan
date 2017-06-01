package org.voovan.tools;

import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.TReflect;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String 工具类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TString {

	private static Hashtable<Integer,Pattern> regexPattern = new Hashtable<Integer,Pattern>();

	/**
	 * 单词首字母大写
	 * @param source 字符串
	 * @return 首字母大写后的字符串
	 */
	public static String uppercaseHead(String source){
		if(source==null){
			return null;
		}
		char[] charArray = source.toCharArray();
		charArray[0] = Character.toUpperCase(charArray[0]);
		return new String(charArray);
	}


	/**
	 * 移除字符串前缀
	 * @param source 目标字符串
	 * @return 移除第一个字节后的字符串
	 */
	public static String removePrefix(String source){
		if(source==null){
			return null;
		}
		return source.substring(1,source.length());
	}
	
	/**
	 * 移除字符串后缀
	 * @param source 目标字符串
	 * @return 移除最后一个字节后的字符串
	 */
	public static String removeSuffix(String source){
		if(source==null){
			return null;
		}

		if(source.isEmpty()){
			return source;
		}
		return source.substring(0, source.length()-1);
	}

	/**
	 * 左补齐
	 * @param source 目标字符串
	 * @param len 补齐后字符串的长度
	 * @param c 用于补齐的字符串
	 * @return 补齐后的字符串
	 */
	public static String leftPad(String source,int len,char c){
		if(source==null){
			source="";
		}
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<len - source.length(); i++){
			sb.append(c);
		}
		return sb.append(source).toString();
	}
	
	/**
	 * 右补齐
	 * @param source 目标字符串
	 * @param len 补齐后字符串的长度
	 * @param c 用于补齐的字符串
	 * @return 补齐后的字符串
	 */
	public static String  rightPad(String source,int len,char c){
		if(source==null){
			source="";
		}
		StringBuffer sb = new StringBuffer();
		sb.append(source);
		for(int i=0; i<len - source.length(); i++){
			sb.append(c);
		}
		return sb.toString();
	}
	
	/**
	 * 判断是否是指定进制的数字字符串
	 * @param numberString  目标字符串
	 * @param radix			进制
	 * @return 是否是指定进制的数字字符串
	 */
	public static boolean isNumber(String numberString,int radix){
		if(numberString==null){
			return false;
		}

		try{
			Integer.parseInt(numberString, radix);
			return true;
		}
		catch(Exception e){
			return false;
		}
	}
	
	/**
	 * 判断是否是整形数
	 * @param integerString 数字字符串
	 * @return 是否是整形数
	 */
	public static boolean isInteger(String integerString){
		if(integerString!=null && regexMatch(integerString, "^-?[0-9]\\d*$")>0){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 判断是否是浮点数
	 * @param floadString  浮点数字符串
	 * @return 是否是浮点数
	 */
	public static boolean isFloat(String floadString){
		if(floadString!=null && regexMatch(floadString, "^-?\\d+\\.\\d+$")>0){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 判断是否是布尔值
	 * @param booleanString 布尔值字符串
	 * @return 是否是布尔值
	 */
	public static boolean isBoolean(String booleanString){
		if( "true".equalsIgnoreCase(booleanString) || "false".equalsIgnoreCase(booleanString)){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 正则表达式查找,匹配的被提取出来做数组
	 * @param source 目标字符串
	 * @param regex 正则表达式
	 * @return  匹配的字符串数组
	 */
	public static String[] searchByRegex(String source,String regex){
		if(source==null){
			return null;
		}

		Pattern pattern = null;
		if(regexPattern.containsKey(regex.hashCode())){
			pattern = regexPattern.get(regex.hashCode());
		}else{
			pattern = Pattern.compile(regex);
			regexPattern.put(regex.hashCode(), pattern);
		}
		Matcher matcher = pattern.matcher(source);
		ArrayList<String> result = new ArrayList<String>();
		while(matcher.find()){
			result.add(matcher.group());
		}
		return result.toArray(new String[0]);
	}

	/**
	 * 正则匹配
	 * @param source 目标字符串
	 * @param regex 正则表达式
     * @return 正则搜索后得到的匹配数量
     */
	public static int regexMatch(String source,String regex){
		return searchByRegex(source,regex).length;
	}
	
	/**
	 * 判断字符串空指针或者内容为空
	 * @param source 字符串
	 * @return 是否是空指针或者内容为空
	 */
	public static boolean isNullOrEmpty(String source){
		if(source==null || source.isEmpty()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 按照标识符 Map 进行替换
	 * @param source		源字符串,标识符使用"{{标识}}"进行包裹,这些标识符将会被替换
	 * @param tokens		标识符Map集合
	 * @return 替换后的字符串
	 */
	public static String tokenReplace(String source,Map<String, String> tokens){
		if(source==null){
			return null;
		}

		for(Entry<String, String> entry : tokens.entrySet()){
			String value = entry.getValue();
			if(value==null){
				value="null";
			}
			source = tokenReplace(source,entry.getKey(),entry.getValue());
		}
		return source;
	}
	
	/**
	 * 按照标识符进行替换
	 * @param source		源字符串,标识符使用"{{标识}}"进行包裹
	 * @param tokenName		标识符
	 * @param tokenValue    标志符值
	 * @return 替换后的字符串
	 */
	public static String tokenReplace(String source,String tokenName,String tokenValue){
		if(source==null){
			return null;
		}

		return source.replaceAll("\\{\\{"+tokenName+"\\}\\}",Matcher.quoteReplacement(tokenValue));
	}

	/**
	 * 按位置格式化字符串
	 * 		TString.format("aaaa{}bbbb{}cccc{}", "1","2","3")
	 * 		输出aaaa1bbbb2cccc3
	 * @param source 字符串
	 * @param args 多个参数
	 * @return 格式化后的字符串
	 */
	public static String tokenReplace(String source,String ...args){
		if(source==null){
			return null;
		}

        source = tokenReplace(source, TObject.arrayToMap(args));
		return source;
	}
	
	/**
	 * 替换第一个标志字符串
	 * @param source  字符串
	 * @param mark    标志字符
	 * @param replacement 替换字符
	 * @return 替换后的结果
	 */
	public static String replaceFirst(String source,String mark,String replacement){
		if(source==null){
			return null;
		}

		int head = source.indexOf(mark);
		int tail = head+mark.length();
		replacement = TObject.nullDefault(replacement,"");
		source = source.substring(0, head)+replacement+source.substring(tail, source.length());
		return source;
	}
	
	/**
	 * 替换最后一个标志字符串
	 * @param source  字符串
	 * @param mark    标志字符
	 * @param replacement 替换字符
	 * @return 替换后的结果
	 */
	public static String replaceLast(String source,String mark,String replacement){
		if(source==null){
			return null;
		}
		int head = source.lastIndexOf(mark);
		int tail = head+mark.length();
		replacement = TObject.nullDefault(replacement,"");
		source = source.substring(0, head)+replacement+source.substring(tail, source.length());
		return source;
	}
	
	/**
	 * 缩进字符串
	 * @param source			待缩进的字符串
	 * @param indentCount   缩进数(空格的数目)
	 * @return 缩进后的字符串
	 */
	public static String indent(String source,int indentCount){
		if(indentCount>0 && source!=null){
			StringBuilder indent = new StringBuilder();
			for(int i=0;i<indentCount;i++){
				indent.append(" ");
			}
			source = indent.toString() + source;
			source = source.replaceAll("\n", "\n" + indent.toString());
		}
		return source;
	}

	/**
	 * 翻转字符串 输入1234 输出4321
	 * @param source  字符串
	 * @return 翻转后的字符串
     */
	public static String reverse(String source){
		if(source!=null){
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
	 *       例如:将字符串中的 \" 转转成 \\\"
	 * @param source 源字符串
	 * @return 转换后的字符串
	 */
	public static String convertEscapeChar(String source){
		if(source==null){
			return null;
		}

		return source.replace("\\","\\u005c")
				.replace("\f","\\u000c")
				.replace("\'","\\u0027")
				.replace("\r","\\u000d")
				.replace("\"","\\u0022")
				.replace("\b","\\u0008")
				.replace("\t","\\u0009")
				.replace("\n","\\u000a");
	}

	/**
	 * 将可在字符串中表达的转义字符,转义成系统转义字符
	 *       例如:将字符串中的 \\\" 转转成 \"
	 * @param source 源字符串
	 * @return 转换后的字符串
	 */
	public static String unConvertEscapeChar(String source){
		if(source==null){
			return null;
		}

		return source.replace("\\\\","\\")
				.replace("\\f","\f")
				.replace("\\'","\'")
				.replace("\\r","\r")
				.replace("\\\"","\"")
				.replace("\\b","\b")
				.replace("\\t","\t")
				.replace("\\n","\n");
	}


	/**
	 * 字符串转 Unicode
	 * @param source 字符串
	 * @return unicode 字符串
	 */
	public static String toUnicode(String source) {

		if(source==null){
			return null;
		}

		StringBuffer result = new StringBuffer();

		for (int i = 0; i < source.length(); i++) {

			// 取出一个字符
			char c = source.charAt(i);

			// 转换为unicode
			result.append("\\u" + leftPad(Integer.toHexString(c),4,'0') );
		}

		return result.toString();
	}

	/**
	 * Unicode 转 字符串
	 * @param source unicode 字符串
	 * @return string 字符串
	 */
	public static String fromUnicode(String source) {
		if(source==null){
			return null;
		}

		if(source.contains("\\u")) {

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
		}else{
			return source;
		}

	}

	/**
	 * 字符串转换为 Java 基本类型
	 * @param value 字符串字面值
	 * @param clazz Class类,仅支持基本类型
	 * @param ignoreCase 是否在字段匹配时忽略大小写
	 * @param <T> 范型
	 * @return 基本类型对象
	 */
	public static <T> T toObject(String value, Class<T> clazz, boolean ignoreCase){
		if(value == null){
			return null;
		}else if(clazz == int.class || clazz == Integer.class){
			return (T)Integer.valueOf(value);
		}else if(clazz == float.class || clazz == Float.class){
			return (T)Float.valueOf(value);
		}else if(clazz == double.class || clazz == Double.class){
			return (T)Double.valueOf(value);
		}else if(clazz == boolean.class || clazz == Boolean.class){
			return (T)Boolean.valueOf(value);
		}else if(clazz == long.class || clazz == Long.class){
			return (T)Long.valueOf(value);
		}else if(clazz == short.class || clazz == Short.class){
			return (T)Short.valueOf(value);
		}else if(clazz == byte.class || clazz == Byte.class){
			return (T)Byte.valueOf(value);
		}else if(clazz == char.class || clazz == Character.class){
			Object tmpValue = value!=null ? value.charAt(0) : null;
			return (T)tmpValue;
		}else if(TReflect.isImpByInterface(clazz,Collection.class) ||
				TReflect.isImpByInterface(clazz,Map.class) ||
				clazz.isArray()){
			return JSON.toObject(value, clazz, ignoreCase);
		}else if(TString.searchByRegex(value,"^\\s*\\{[\\s\\S]*\\}\\s*$").length > 0
				|| TString.searchByRegex(value,"^\\s*\\[[\\s\\S]*\\]\\s*$").length > 0 ){
			return JSON.toObject(value, clazz, ignoreCase);
		}else{
			return (T)value;
		}
	}

	/**
	 * 字符串转换为 Java 基本类型
	 * @param value 字符串字面值
	 * @param clazz Class类,仅支持基本类型
	 * @param <T> 范型
	 * @return 基本类型对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T toObject(String value,Class<T> clazz){
		return (T)toObject(value, clazz, false);
	}



	private static String[] chars = new String[] { "a", "b", "c", "d", "e", "f",
			"g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s",
			"t", "u", "v", "w", "x", "y", "z", "0", "1", "2", "3", "4", "5",
			"6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I",
			"J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
			"W", "X", "Y", "Z" };

	/**
	 * 生成短 UUID
	 * @return 生成的短 UUID
	 */
	public static String generateShortUUID() {
		StringBuffer shortBuffer = new StringBuffer();
		String uuid = UUID.randomUUID().toString().replace("-", "");
		for (int i = 0; i < 8; i++) {
			String str = uuid.substring(i * 4, i * 4 + 4);
			int x = Integer.parseInt(str, 16);
			shortBuffer.append(chars[x % 0x3E]);
		}
		return shortBuffer.toString();

	}
}
