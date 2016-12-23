package org.voovan.tools.json;

import com.sun.javafx.collections.MappingChange;
import org.voovan.tools.TObject;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.TString;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.*;

/**
 * JSON字符串分析成 Map
 * 
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSONDecode {
	
	/**
	 * 解析 JSON 字符串
	 * 		如果是{}包裹的对象解析成 HashMap,如果是[]包裹的对象解析成 ArrayList
	 * @param jsonStr	待解析的 JSON 字符串
	 * @return 解析后的对象
	 */
	public static Object parse(String jsonStr){

		if(jsonStr==null){
			return null;
		}

		jsonStr = removeComment(jsonStr);
		Object jsonResult = null;
		//处理掉注释
		jsonStr = jsonStr.replaceAll("\\/\\/[^\n]*", "");
		jsonStr = jsonStr.replaceAll("/\\*([^\\*^/]*|[\\*^/*]*|[^\\*/]*)*\\*/", "");
		//处理掉前后的特殊字符
		jsonStr = jsonStr.trim();
		//根据起始和结束符号,决定返回的对象类型
		if(jsonStr.startsWith("{")){
			jsonResult = (Map)new HashMap<String,Object>();
		}
		else if(jsonStr.startsWith("[")){
			jsonResult = (List)new ArrayList<Object>();
		}
		//规范化字符串
		jsonStr = jsonStr.replaceAll("\\s*:\\s*", ":");
		jsonStr = jsonStr.substring(1,jsonStr.length()-1);
		String keyString = null;
		Object value = null;
		int arrayWarpFlag = 0;
		int objectWarpFlag = 0;
		int stringWarpFlag = 0;
		boolean isString = false;
		boolean isArray = false;
		boolean isObject = false;
		boolean isFunction =false;
		StringBuilder itemString = new StringBuilder();
		
		for(int i=0;i<jsonStr.length();i++){
			char currentChar = jsonStr.charAt(i);
			
			itemString.append(currentChar);
			
			//分析字符串,如果是字符串不作任何处理
			if(currentChar=='\"'){
				//i小于1的不是转意字符,判断为字符串(因为转意字符要2个字节),大于2的要判断是否\\"的转义字符
				if(i<=1 || !(i>1 && jsonStr.charAt(i-1)=='\\')){
					stringWarpFlag++;
					//字符串起始的"
					if(stringWarpFlag==1 && itemString.toString().trim().startsWith("\"")){
						isString = true;
					}
					//字符串结束的"
					else if(stringWarpFlag==2){
						stringWarpFlag=0;
						isString = false;
					}
				}
			}
			//如果是函数 function 起始
			else if(!isString && !isObject && itemString.toString().trim().startsWith("function")){

				if(currentChar=='{'){
					objectWarpFlag++;
				}
				else if(currentChar=='}'){
					objectWarpFlag -- ;

					if(objectWarpFlag==0){
						isFunction = false;
						value = itemString.toString();
						itemString = new StringBuilder();
					}
				} else {
					isFunction = true;
				}
			}
			//JSON数组字符串分组,以符号对称的方式取 []
			else if(!isString && !isObject && !isFunction && currentChar=='['){
				arrayWarpFlag++;
				isArray = true;
			}
			else if(!isString && !isObject && !isFunction && currentChar==']'){
				arrayWarpFlag--;
				if(arrayWarpFlag==0){
					//递归解析处理,取 value 对象
					value = JSONDecode.parse(itemString.toString());
					itemString = new StringBuilder();
					isArray = false;
					i++;
				}
			}
			//JSON对象字符串分组,以符号对称的方式取 {}
			else if(!isString && !isArray && !isFunction && currentChar=='{'){
				objectWarpFlag++;
				isObject = true;
			}
			else if(!isString && !isArray && !isFunction && currentChar=='}'){
				objectWarpFlag--;
				if(objectWarpFlag==0){
					//递归解析处理,取 value 对象
					value = JSONDecode.parse(itemString.toString());
					itemString = new StringBuilder();
					isObject = false;
					i++;
				}
			}
			//JSON对象字符串分组,取 Key 对象,当前字符是:则取 Key
			else if(!isString && !isObject && !isArray && !isFunction && currentChar==':'){
				keyString = itemString.substring(0,itemString.length()-1).trim();
				itemString = new StringBuilder();
			}
			//JSON对象字符串分组,取 value 对象,当前字符是,则取 value
			else if(!isString && !isArray && !isObject && !isFunction && currentChar==','){
				value = itemString.substring(0,itemString.length()-1).trim();
				itemString = new StringBuilder();
			}
			//最后结尾的是没有,号分割的,特殊处理
			if(jsonStr.length() == i+1 && itemString.length()!=0){
				value = itemString.toString().trim();
				if(value.toString().isEmpty()){
					break;
				}
			}
			
			//返回值处理
			if(value!=null){
				//判断取值不是任何对象
				if(value instanceof String){
					String stringValue = TObject.cast(value);

					//判断是字符串去掉头尾的冒号
					if(stringValue.startsWith("\"") && stringValue.endsWith("\"")){
						value = stringValue.substring(1,stringValue.length()-1);
						value = value.toString().replace("\\\\","\\").replace("\\r","\r").replace("\\n","\n");
					}
					//判断不包含.即为整形
					else if (TString.isInteger(stringValue)){
						Long longValue = Long.parseLong((String)value);
						if(longValue <= 2147483647 && longValue >= -2147483647){
							value = Integer.parseInt((String)value);
						}else{
							value = longValue;
						}
					}
					//判断有一个.即为浮点数,转换成 Float
					else if (TString.isFloat(stringValue)) {
						value = new Float((String)value);
					}
					//判断是否是 boolean 类型
					else if(TString.isBoolean(stringValue)){
						value = Boolean.parseBoolean((String)value);
					}else if(value.equals("null")){
						value = null;
					}
				}
				
				//这里 key 和 value 都准备完成了
				
				//判断返回对象的类型,填充返回对象
				if(jsonResult instanceof HashMap){
					@SuppressWarnings("unchecked")
					HashMap<String, Object> result = (HashMap<String, Object>)jsonResult;
					if(keyString!=null) {
						//容错,如果是双引号包裹的则去除首尾的双引号
						if(keyString.startsWith("\"") && keyString.endsWith("\"")){
							keyString = keyString.substring(1, keyString.length() - 1);
						}
						result.put(keyString, value);
					}
				}
				if(jsonResult instanceof ArrayList && value != null){
					@SuppressWarnings("unchecked")
					ArrayList<Object> result = (ArrayList<Object>)jsonResult;
					result.add(value);
				}
				//处理完侯将 value 放空
				keyString = null;
				value = null;
			}
		}
		return jsonResult;
	}
	
	/**
	 * 解析 JSON 字符串成为参数指定的类
	 * @param <T> 		范型
	 * @param jsonStr	JSON字符串
	 * @param clazz		JSON 字符串将要转换的目标类
	 * @return					JSON 转换后的 Java 对象
	 * @throws ReflectiveOperationException  反射异常
	 * @throws ParseException 解析异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T fromJSON(String jsonStr,Class<T> clazz) throws ReflectiveOperationException, ParseException{
		if(jsonStr==null){
			return null;
		}

		jsonStr = removeComment(jsonStr);
		Object parseObject = parse(jsonStr);
		//{}包裹的对象处理
		if(TString.searchByRegex(jsonStr,"^\\s*\\{[\\s\\S]*\\}\\s*$").length > 0 ){
			Map<String,Object> mapJSON = (Map<String, Object>) parseObject;
			return (T) TReflect.getObjectFromMap(clazz, mapJSON,false);
		}
		//[]包裹的对象处理
		else if(TString.searchByRegex(jsonStr,"^\\s*\\[[\\s\\S]*\\]\\s*$").length > 0){
			return (T) TReflect.getObjectFromMap(clazz, TObject.newMap("value",parseObject),false);
		}
		//其他类型处理
		else{
			return null;
		}
	}
	
	private static String removeComment(String jsonStr){
		if(jsonStr==null){
			return null;
		}
		//处理掉注释
		jsonStr = jsonStr.replaceAll("\\/\\/[^\n]*", "");
		jsonStr = jsonStr.replaceAll("/\\*([^\\*^/]*|[\\*^/*]*|[^\\*/]*)*\\*/", "");
		jsonStr = jsonStr.trim();
		return jsonStr;
	}
}
