package org.voovan.tools.json;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.collection.IntKeyMap;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

/**
 * JSON字符串分析成 Map
 * 1.多行文本
 * 2.行尾不精确效验逗号
 * 3.键值对冒号可用等号代替
 * 4.非精确效验双引号包裹
 * 5.可使用单双引号进行包裹
 * 6.支持 JAVA/C 语言的两种形式的注释以及井号 形式的注释
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSONDecode {
	public static boolean JSON_HASH = TEnv.getSystemProperty("JsonHash", false);
	public final static IntKeyMap<Object> JSON_DECODE_CACHE = new IntKeyMap<Object>(1024);

	static {
		if(JSON_HASH) {
			Global.getHashWheelTimer().addTask(new HashWheelTask() {
				@Override
				public void run() {
					JSON_DECODE_CACHE.clear();
				}
			}, 1);
		}
	}

	private static int E_OBJECT = 1;
	private static int E_ARRAY = -1;

	public static Object parse(String jsonStr) {
		Object value;
		int jsonHash = 0;
		if(JSON_HASH) {
			jsonHash = THash.HashFNV1(jsonStr);
			value = JSON_DECODE_CACHE.get(jsonHash);

			if (value != null) {
				return value;
			}
		}

		value = parse(new StringReader(jsonStr.trim()));

		if(JSON_HASH) {
			JSON_DECODE_CACHE.put(jsonHash, value);
		}

		return value;
	}

	/**
	 * 创建根对象
	 * @param reader StringReader 对象
	 * @return 根对象
	 * @throws IOException IO 异常
	 */
	public static Object createRootObj (StringReader reader) throws IOException {
		int type = 0;
		Object root = null;

		//根据起始和结束符号,决定返回的对象类型
		if (type == 0) {
			char flag = (char) reader.read();

			if (flag == Global.CHAR_LC_BRACES) {
				type = E_OBJECT;
			}

			if (flag == Global.CHAR_LS_BRACES) {
				type = E_ARRAY;
			}
		}

		//对象类型构造返回的对象
		if (E_OBJECT == type) {
			root = (Map) new LinkedHashMap<String, Object>(1024);
		} else if (E_ARRAY == type) {
			root = (List) new ArrayList<Object>(1024);
		} else {
			reader.skip(-1);
		}

		return root;
	}

	/**
	 * 解析 JSON 字符串
	 *         如果是{}包裹的对象解析成 HashMap,如果是[]包裹的对象解析成 ArrayList
	 * @param reader    待解析的 JSON 字符串
	 * @return 解析后的对象
	 */
	private static Object parse(StringReader reader) {

		try {

			if (reader == null) {
				return null;
			}

			Object root = null;

			String keyString = null;
			Object value = null;
			char stringWarpFlag = '\0';
			int functionWarpFlag = 0;
			boolean isString = false;
			boolean isFunction = false;
			int isComment = 0;
			StringBuilder itemString = new StringBuilder();

			char currentChar = 0;
			char nextChar = 0;
			char prevChar = 0;

			while (true) {
				currentChar = (char) reader.read();

				reader.mark(0);
				nextChar = (char) reader.read();
				reader.reset();

				if (reader.skip(-2)==-2) {
					prevChar = (char) reader.read();
				}
				reader.reset();

				//====================  处理注释  ====================
				if (!isString) {
					//单行注释, # ......
					if (currentChar == Global.CHAR_SHAPE){
						isComment = 1;
					}

					if(currentChar == Global.CHAR_BACKSLASH && isComment == 0) {
						//单行注释 like: // ......
						if (nextChar != 0 && nextChar == Global.CHAR_BACKSLASH){
							isComment = 1;
						}

						//多行注释, like: /* ...... */
						if (nextChar != 0 && nextChar == Global.CHAR_STAR) {
							isComment = 2;
							if (currentChar == 65535) {
								return root;
							}
							continue;
						}
					}

					if(isComment > 0) {
						//单行注释结束
						if (isComment == 1 && currentChar == Global.CHAR_LF) {
							isComment = 0;
						}

						//多行注释结束
						if (isComment == 2 && currentChar == Global.CHAR_BACKSLASH && (prevChar != 0 && prevChar == Global.CHAR_STAR)) {
							isComment = 0;
							if (currentChar == 65535) {
								return root;
							}
							continue;
						}

						if (currentChar == 65535) {
							return root;
						}
						continue;
					}
				}

				//====================  创建根对象((有根包裹)  ====================
				if (root == null && !isString && isComment==0 && !isFunction) {
					if(currentChar == Global.CHAR_LS_BRACES || currentChar == Global.CHAR_LC_BRACES) {
						reader.skip(-1);
						root = createRootObj(reader);
						continue;
					}
				}

				//====================  处理字符串  ====================
				//分析字符串,如果是字符串不作任何处理
				if (currentChar == Global.CHAR_QUOTE || currentChar == Global.CHAR_S_QUOTE) {
					//非注释状态, 并且不是转移字符
					if (isComment==0 && prevChar != Global.CHAR_SLASH) {
						//字符串起始的 " 或 '
						if (stringWarpFlag == Global.CHAR_EOF) {
							stringWarpFlag = currentChar;
							isString = true;
						}

						//字符串结束的"
						else if (stringWarpFlag != Global.CHAR_EOF && currentChar == stringWarpFlag) {
							stringWarpFlag = Global.CHAR_EOF;
							isString = false;
						}
					}
				}


				//====================  处理对象的包裹  ====================
				if(!isString &&  !isFunction) {
					//数组 []
					if (currentChar == Global.CHAR_LS_BRACES) {
						reader.skip(-1);
						//递归解析处理,取 value 对象
						value = JSONDecode.parse(reader);
						continue;
					} else if (currentChar == Global.CHAR_RS_BRACES) {
						//最后一个元素,追加一个,好将其附加到结果集
						if (itemString.length() != 0 || value != null) {
							currentChar = Global.CHAR_COMMA;
							reader.skip(-1);
						} else {
							return root;
						}
					}

					//对象 {}
					else if (currentChar == Global.CHAR_LC_BRACES) {
						reader.skip(-1);
						//递归解析处理,取 value 对象
						value = JSONDecode.parse(reader);
						continue;
					} else if (currentChar == Global.CHAR_RC_BRACES) {
						//最后一个元素,追加一个,号来将其附加到结果集
						if (itemString.length() != 0 || value != null) {
							currentChar = Global.CHAR_COMMA;
							reader.skip(-1);
						} else {
							return root;
						}
					}
				}

				//如果为字符串则无条件拼装
				//如果不是字符串,则只拼装可见字符
				if (isString || (!isString && !Character.isWhitespace(currentChar))) {
					itemString.append(currentChar);
				}

				//处理数据
				if(!isString) {
					//如果是函数 function 起始
					if (!isString && itemString.toString().trim().startsWith("function")) {

						if (currentChar == Global.CHAR_LC_BRACES) {
							functionWarpFlag++;
						} else if (currentChar == Global.CHAR_RC_BRACES) {
							functionWarpFlag--;

							if (functionWarpFlag == 0) {
								isFunction = false;
								value = itemString.toString();
								itemString = new StringBuilder();
							}
						} else {
							isFunction = true;
						}
					}

					if(!isFunction) {
						//JSON对象字符串分组,取 Key 对象,当前字符是:则取 Key
						if (currentChar == Global.CHAR_COLON || currentChar == Global.CHAR_EQUAL) {
							keyString = itemString.substring(0, itemString.length() - 1).trim();
							itemString = new StringBuilder();
						}

						//JSON对象字符串分组,取 value 对象,当前字符是,则取 value
						if (currentChar == Global.CHAR_COMMA) {
							if (value == null) {
								value = itemString.substring(0, itemString.length() - 1).trim();
							}
							itemString = new StringBuilder();
						}

						//JSON对象字符串分组,取 value 对象,当前字符是换行, 则取 value
						if (currentChar == '\n') {
							itemString.trimToSize();
							if (value == null && itemString.length() > 0) {
								value = itemString.toString().trim();
							}
							itemString = new StringBuilder();
						}
					}
				}

				//返回值处理
				if (value != null) {
					//判断取值不是任何对象
					if (value instanceof String) {
						String stringValue = (String)value;

						//判断是字符串去掉头尾的包裹符号
						if (stringValue.length() >= 2 && stringValue.charAt(0) == Global.CHAR_QUOTE && stringValue.charAt(stringValue.length()-1) == Global.CHAR_QUOTE) {
							value = stringValue.substring(1, stringValue.length() - 1);
							if(JSON.isConvertEscapeChar()) {
								value = TString.unConvertEscapeChar(value.toString());
							}
						}
						//判断是字符串去掉头尾的包裹符号
						else if (stringValue.length() >= 2 && stringValue.charAt(0) == Global.CHAR_S_QUOTE && stringValue.charAt(stringValue.length()-1) == Global.CHAR_S_QUOTE) {
							value = stringValue.substring(1, stringValue.length() - 1);
							if(JSON.isConvertEscapeChar()) {
								value = TString.unConvertEscapeChar(value.toString());
							}
						}
						//判断科学技术转换成 BigDecimal
						else if (stringValue.indexOf(".")>0 && stringValue.indexOf("E")>0) {
							value = new BigDecimal(stringValue);
						}
						//判断不包含.即为整形
						else if (TString.isInteger(stringValue)) {
							Long longValue = Long.parseLong((String) value);
							if (longValue <= 2147483647 && longValue >= -2147483647) {
								value = Integer.parseInt((String) value);
							} else {
								value = longValue;
							}
						}
						//判断有一个.即为浮点数,转换成 Float
						else if (TString.isDecimal(stringValue)) {
							Object resultValue = new Float((String) value);
							if(resultValue.toString().equals(value)){
								value = resultValue;
							} else {

								resultValue = new Double((String) value);
								if (resultValue.toString().equals(value)) {
									value = resultValue;
								} else {
									resultValue = new BigDecimal((String) value);
								}
							}

							value = resultValue;

						}
						//判断是否是 boolean 类型
						else if (TString.isBoolean(stringValue)) {
							value = Boolean.parseBoolean((String) value);
						} else if (value.equals("null")) {
							value = null;
						} else if(value.equals("")){
							value = null;
						}
					}

					//====================  创建根对象(无根包裹)  ====================
					if(root == null) {
						if(keyString!=null) {
							root = (Map) new LinkedHashMap<String, Object>(1024);
						} else {
							root = (List) new ArrayList<Object>(1024);
						}
					}

					//判断返回对象的类型,填充返回对象
					if (root instanceof HashMap) {
						if (keyString != null) {
							//判断是字符串去掉头尾的包裹符号
							if (keyString.length() >= 2 && keyString.charAt(0) == Global.CHAR_QUOTE && keyString.charAt(keyString.length()-1) == Global.CHAR_QUOTE) {
								keyString = keyString.substring(1, keyString.length() - 1);
							}
							//判断是字符串去掉头尾的包裹符号
							if (keyString.length() >= 2 && keyString.charAt(0) == Global.CHAR_S_QUOTE && keyString.charAt(keyString.length()-1) == Global.CHAR_S_QUOTE) {
								keyString = keyString.substring(1, keyString.length() - 1);
							}
							((Map)root).put(keyString, value);
						}
					} else if (root instanceof ArrayList && value != null) {
						((List)root).add(value);
					} else if(root == null){
						root = value;
					}
					//处理完侯将 value 放空
					keyString = null;
					value = null;
				}

				if (nextChar == 65535) {
					if(root==null && value == null && keyString == null) {
						root = itemString.toString();
					}
					break;
				}
			}

			return root;
		} catch(Exception e){
			try {
				int position = ((int) TReflect.getFieldValue(reader, "next") -1);
				String jsonStr = (String) TReflect.getFieldValue(reader, "str");
				jsonStr = jsonStr.substring(0, position)+"^"+jsonStr.substring(position, position+10);
				Logger.error(jsonStr, e);
			} catch (ReflectiveOperationException ex) {
				Logger.error(ex);
			}

			return null;

		}
	}

	/**
	 * 解析 JSON 字符串成为参数指定的类
	 * @param <T>         范型
	 * @param jsonStr    JSON字符串
	 * @param type        JSON 字符串将要转换的目标类
	 * @param ignoreCase 是否在字段匹配时忽略大小写
	 * @return                    JSON 转换后的 Java 对象
	 * @throws ReflectiveOperationException  反射异常
	 * @throws ParseException 解析异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T fromJSON(String jsonStr, Type type, boolean ignoreCase) throws ReflectiveOperationException, ParseException {
		if(jsonStr==null){
			return null;
		}

		Object parseObject = parse(jsonStr);

		if(parseObject == null){
			parseObject = jsonStr;
		}

		if(type == Map.class && parseObject instanceof Map){
			return (T)parseObject;
		}

		//{}包裹的对象处理
		else if(parseObject instanceof Map){
			Map<String,Object> mapJSON = (Map<String, Object>) parseObject;
			return (T) TReflect.getObjectFromMap(type, mapJSON,ignoreCase);
		}
		//[]包裹的对象处理
		else if(parseObject instanceof Collection){
			return (T) TReflect.getObjectFromMap(type, TObject.asMap(TReflect.SINGLE_VALUE_KEY, parseObject),false);
		}
		//如果传入的是标准类型则尝试用TString.toObject进行转换
		else if(parseObject instanceof String || parseObject.getClass().isPrimitive()){
			return TString.toObject(parseObject.toString(), type);
		}
		//其他类型处理
		else{
			return null;
		}
	}

	/**
	 * 解析 JSON 字符串成为参数指定的类,默认严格限制字段大小写
	 * @param <T>         范型
	 * @param jsonStr    JSON字符串
	 * @param clazz        JSON 字符串将要转换的目标类
	 * @param clazz            转换的目标 java 类
	 * @return                    JSON 转换后的 Java 对象
	 * @throws ReflectiveOperationException  反射异常
	 * @throws ParseException 解析异常
	 * @throws IOException IO 异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T fromJSON(String jsonStr,Class<T> clazz) throws ParseException, ReflectiveOperationException, IOException {
		return fromJSON(jsonStr, clazz, false);
	}
}
