package org.voovan.tools;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
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
	 * 移除字符串前缀
	 * @param source 目标字符串
	 * @return 移除第一个字节后的字符串
	 */
	public static String removePrefix(String source){
		return source.substring(1,source.length());
	}
	
	/**
	 * 移除字符串后缀
	 * @param source 目标字符串
	 * @return 移除最后一个字节后的字符串
	 */
	public static String removeSuffix(String source){
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
	 * @return
	 */
	public static boolean isNumber(String numberString,int radix){
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
	 * @param integerString
	 * @return
	 */
	public static boolean isInteger(String integerString){
		if(searchByRegex(integerString, "^-?[0-9]\\d*$").length>0){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 判断是否是浮点数
	 * @param floadString
	 * @return
	 */
	public static boolean isFloat(String floadString){
		if(searchByRegex(floadString, "^-?[1-9]\\d*\\.\\d*|-0\\.\\d*[1-9]\\d*$").length>0){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 判断是否是浮点数
	 * @param floadString
	 * @return
	 */
	public static boolean isBoolean(String floadString){
		if("true".equalsIgnoreCase(floadString) || "false".equalsIgnoreCase(floadString)){
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
	 * 判断字符串空指针或者内容为空
	 * @param source
	 * @return
	 */
	public static boolean isNullOrEmpty(String source){
		if(source==null || source.equals("")){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 按照标识符 Map 进行替换
	 * @param source		源字符串,标识符使用"{{标识}}"进行包裹,这些标识符将会被替换
	 * @param tokens		标识符Map集合
	 * @return
	 */
	public static String tokenReplace(String source,Map<String, String> tokens){
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
	 * @return
	 */
	public static String tokenReplace(String source,String tokenName,String tokenValue){
		return source.replaceAll("\\{\\{"+tokenName+"\\}\\}",Matcher.quoteReplacement(tokenValue));
	}
	
	/**
	 * 按位置格式化字符串
	 * 		TString.format("aaaa{}bbbb{}cccc{}", "1","2","3")
	 * 		输出aaaa1bbbb2cccc3
	 * @param source
	 * @param args
	 * @return
	 */
	public static String format(String source,String ...args){
		for(String arg : args){
			source = replaceFirst(source,"{}",arg);
		}
		return source;
	}
	
	/**
	 * 替换第一个标志字符串
	 * @param source
	 * @param mark
	 * @param replacement
	 * @return
	 */
	public static String replaceFirst(String source,String mark,String replacement){
		int head = source.indexOf(mark);
		int tail = head+mark.length();
		replacement = replacement==null?"":replacement;
		source = source.substring(0, head)+replacement+source.substring(tail, source.length());
		return source;
	}
	
	/**
	 * 替换最后一个标志字符串
	 * @param source
	 * @param mark
	 * @param replacement
	 * @return
	 */
	public static String replaceLast(String source,String mark,String replacement){
		
		int head = source.lastIndexOf(mark);
		int tail = head+mark.length();
		replacement = replacement==null?"":replacement;
		source = source.substring(0, head)+replacement+source.substring(tail, source.length());
		return source;
	}
	
	/**
	 * 缩进字符串
	 * @param str			待缩进的字符串
	 * @param indentCount   缩进数(空格的数目)
	 * @return
	 */
	public static String indent(String str,int indentCount){
		if(indentCount>0 && str!=null){
			String indent = "";
			for(int i=0;i<indentCount;i++){
				indent += " ";
			}
			str = indent + str;
			str = str.replaceAll("\n", "\n" + indent);
		}
		return str;
	}

}
