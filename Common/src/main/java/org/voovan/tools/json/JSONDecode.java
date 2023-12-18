package org.voovan.tools.json;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.voovan.Global;
import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.collection.IntKeyMap;
import org.voovan.tools.exception.ParseException;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;

/**
 * JSON字符串分析成 Map
 * 1.多行文本
 * 2.行尾不精确效验逗号
 * 3.键值对冒号可用等号代替
 * 4.非精确效验双引号包裹
 * 5.可使用单双引号进行包裹
 * 6.支持 JAVA/C 语言的两种形式的注释以及井号 形式的注释
 * 7.支持 HCL 配置解析
 * 8. 同源文件引用: 使用 '@' 标记进行同源引用从 application.json 所在路径作为上下文引入文件
 * 9. URL资源医用: 使用完整的 URL 连接方式引入,即 http[s]://, file://, ftp:// 等方式引入
 * 10.使用 |token| 的方式进行差值替换操作, 使用 '^'' 作为引入上一级, '.' 符号作为当前级别
 * 11.支持同名(相同key)的 Map 和 List 的数据合并
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSONDecode {
	public static boolean JSON_HASH = TEnv.getSystemProperty("JsonHash", false);

	//JSON URL 引用时用来记录上下文地址, 默认当前工作目录
	protected static FastThreadLocal<String> CONTEXT_PATH = FastThreadLocal.withInitial(new Supplier<String>() {
		@Override
		public String get() {
			return TFile.getContextPath();
		}
	});

	protected static FastThreadLocal<Object> ROOT = new FastThreadLocal<Object>();

	public final static IntKeyMap<Object> JSON_DECODE_CACHE = new IntKeyMap<Object>(1024);

	static {
		if(JSON_HASH) {
			Global.schedual(new HashWheelTask() {
				@Override
				public void run() {
					JSON_DECODE_CACHE.clear();
				}
			}, 1);
		}
	}

	private static int E_OBJECT = 1;
	private static int E_ARRAY = -1;


	/**
	 * 基于字符串的解析方法
	 * @param jsonStr json 字符串
	 * @param enableToken 开启插值功能
	 * @param enablbeRef 开启引用功能
	 * @return json 解析出的对象
	 */
	public static Object parse(String jsonStr, boolean enableToken, boolean enablbeRef) {
		Object value;
		int jsonHash = 0;
		if(JSON_HASH) {
			jsonHash = THash.HashFNV1(jsonStr);
			value = JSON_DECODE_CACHE.get(jsonHash);

			if (value != null) {
				return value;
			}
		}

		value = parse(new StringReader(jsonStr.trim()), enableToken, enablbeRef, null, null);

		if(JSON_HASH) {
			JSON_DECODE_CACHE.put(jsonHash, value);
		}

		ROOT.set(null);

		return value;
	}

	/**
	 * 基于字符串的解析方法
	 * @param jsonStr 字符串
	 * @return json 解析出的对象
	 */
	public static Object parse(String jsonStr) {
		return parse(jsonStr, false, false);
	}


	/**
	 * 基于文件的解析方法
	 * @param file 基于文件的解析方法
	 * @param enableToken 开启插值功能
	 * @param enablbeRef 开启引用功能
	 * @return json 解析出的对象
	 */
	private static Object parse(File file, boolean enableToken, boolean enablbeRef) {
		if(file.exists()) {
			String jsonContent = new String(TFile.loadFile(file));
			return parse(new StringReader(jsonContent), enableToken, enablbeRef, null, null);
		} else {
			return null;
		}
	}

	/**
	 * 基于URL的解析方法
	 * @param file 基于文件的解析方法
	 * @param enableToken 开启插值功能
	 * @param enablbeRef 开启引用功能
	 * @param parentPath 上级的 key 字符串
	 * @param parentRoot 上级的 root 节点
	 * @return json 解析出的对象
	 */
	private static Object parse(URL url, boolean enableToken, boolean enablbeRef, String parentPath, Object parentRoot) throws IOException {
		String jsonContent = TString.loadURL(url.toString());
		return parse(new StringReader(jsonContent), enableToken, enablbeRef, parentPath, parentRoot);
	}


	/**
	 * 创建根对象
	 * @param flag 用户创建跟对象的标记
	 * @return 根对象
	 * @throws IOException IO 异常
	 */
	public static Object createRootObj(char flag) throws IOException {
		int type = 0;
		Object root = null;

		//根据起始和结束符号,决定返回的对象类型
		if (type == 0) {
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
			Logger.errorf("JSONDecode: create root objec failed {}", flag);
		}

		//为插值替换做准备
		if(ROOT.get()==null) {
			ROOT.set(root);
		}

		return root;
	}

	/**
	 * 解析 JSON 字符串
	 *         如果是{}包裹的对象解析成 HashMap,如果是[]包裹的对象解析成 ArrayList
	 * @param reader    待解析的 JSON 字符串
	 * @param enableToken 开启插值功能
	 * @param enablbeRef 开启引用功能
	 * @param parentPath 上级的 key 字符串
	 * @param parentRoot 上级的 root 节点
	 * @return 解析后的对象
	 */
	private static Object parse(StringReader reader, boolean enableToken, boolean enablbeRef, String path, Object parentRoot) {

		try {

			if (reader == null) {
				return null;
			}

			Object root = null;
			String parentPath = path == null ? "" : path;
			String parentKey = null;
			if(!parentPath.isEmpty()) {
				int index = parentPath.lastIndexOf(".");
				parentKey = parentPath.substring(index+1);
			}

			String key = null;
			Object value = null;
			char stringWarpFlag = '\0';
			boolean stringMode = false;
			int commentMode = 0;

			StringBuilder itemString = new StringBuilder();

			char currentChar = 0;
			char nextChar = 0;
			char prevChar = 0;
			boolean isConvertChar = false; //是否处于转移字符状态

			boolean filledByRecursion = false;

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
				if (!stringMode) {
					//单行注释开始, # ......
					if (currentChar == Global.CHAR_SHAPE){
						commentMode = 1;
					}

					if(currentChar == Global.CHAR_BACKSLASH && commentMode == 0) {
						//单行注释开始, like: // ......
						if (nextChar != 0 && nextChar == Global.CHAR_BACKSLASH){
							commentMode = 1;
						}

						//多行注释开始, like: /* ...... */
						if (nextChar != 0 && nextChar == Global.CHAR_STAR) {
							commentMode = 2;
							continue;
						}
					}

					if(currentChar == Global.CHAR_SHAPE && commentMode==0) {
						if (nextChar != 0){
							commentMode = 1;
						}
					}

					//在无逗号结尾的行,带有注释时准确区分数据
					if(itemString.length()>0 && commentMode>0) {
						reader.skip(-1);
						nextChar = currentChar;
						currentChar = ',';
						commentMode = 0;
					}

					if(commentMode > 0) {
						//单行注释结束
						if (commentMode == 1 && currentChar == Global.CHAR_LF) {
							commentMode = 0;
						}

						//多行注释结束
						if (commentMode == 2 && currentChar == Global.CHAR_BACKSLASH && (prevChar != 0 && prevChar == Global.CHAR_STAR)) {
							commentMode = 0;
							continue;
						}

						if (nextChar == 65535) {
							return root;
						}
						continue;
					}
				}

				//==================== 第一处  创建根对象(有根包裹)  ====================
				if (root == null && !stringMode && commentMode==0) {
					if(currentChar == Global.CHAR_LS_BRACES || currentChar == Global.CHAR_LC_BRACES || currentChar == Global.CHAR_COLON) {
						char flag = currentChar;

						//如果之前有解析的字符, 则判断为 Map 类型
						//因为 key [] 或者 key {} 的形式在到达指定符号前有解析的 key 字符
						if(itemString.length() > 0) {
							flag = '{';
						}

						//获取上一级的value作为当前级的root
						if(parentKey!=null && parentRoot instanceof Map) {
							root = ((Map)parentRoot).get(parentKey);
						}
						
						//如果上一级没有则创建root
						if(root==null) {
							//通过结构形式推断根对象类型
							root = createRootObj(flag);
						}

						//增加回写, 将数据回写至上层
						if(parentRoot instanceof List) {
							((List) parentRoot).add(root);
						}

						if(parentRoot instanceof Map && parentKey!=null) {
							((Map) parentRoot).put(parentKey, root);
						}

						//推断根对象类型, 则字符不表意, 则继续处理
						if(currentChar == Global.CHAR_LS_BRACES || currentChar == Global.CHAR_LC_BRACES) {
							if(itemString.length() >0) {
								reader.skip(-1);
							}
							continue;
						}
					}
				}

				//====================  处理字符串  ====================
				//分析字符串,如果是字符串不作任何处理
				if (currentChar == Global.CHAR_QUOTE || currentChar == Global.CHAR_S_QUOTE) {
					//非注释状态, 并且不是转移字符
					if (commentMode==0 && !isConvertChar) {
						//字符串起始的 " 或 '
						if (stringWarpFlag == Global.CHAR_EOF) {
							stringWarpFlag = currentChar;
							stringMode = true;
						}

						//字符串结束的"
						else if (stringWarpFlag != Global.CHAR_EOF && currentChar == stringWarpFlag) {
							stringWarpFlag = Global.CHAR_EOF;
							stringMode = false;
						}
					}
				}


				//====================  处理对象的包裹  ====================
				if(!stringMode) {
					//数组 [] 处理
					if (currentChar == Global.CHAR_LS_BRACES) {
						reader.skip(-1);

						//支持{ key [...] }的形式, (没有使用 [:,=] 分割 key/value) 插入一个 : 作为分割
						if(itemString.length() >0) {
							nextChar = currentChar;
							currentChar = Global.CHAR_COLON;
						} else {
							//递归解析处理,取 value 对象
							String tmpPath = key ==null ? parentPath : (parentPath.isEmpty()? key : parentPath+"."+key);
							value = JSONDecode.parse(reader, enableToken, enablbeRef, tmpPath, root);
							filledByRecursion = true;
							continue;
						}

					} else if (currentChar == Global.CHAR_RS_BRACES) {
						//最后一个元素,追加一个,好将其附加到结果集
						if (itemString.length() != 0 || value != null) {
							currentChar = Global.CHAR_COMMA;
							reader.skip(-1);
						} else {
							return root;
						}
					}

					//对象 {} 处理
					else if (currentChar == Global.CHAR_LC_BRACES) {
						reader.skip(-1);

						//支持{ key {...} }的形式(没有使用 [:,=] 分割 key/value), 插入一个 : 作为分割
						if(itemString.length() >0) {
							nextChar = currentChar;
							currentChar = ':';
						} else {
							//递归解析处理,取 value 对象
							String tmpPath = key ==null ? parentPath : (parentPath.isEmpty()? key : parentPath+"."+key);
							value = JSONDecode.parse(reader, enableToken, enablbeRef, tmpPath, root);
							filledByRecursion = true;
							continue;
						}
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

				//拼装字符串
				{
					if(currentChar == Global.CHAR_SLASH && !isConvertChar) {
						isConvertChar = true;
					}

					if(prevChar == Global.CHAR_SLASH  && isConvertChar) {
						isConvertChar = false;
					}

					//stringMode 无条件拼装
					//itemString.length==0 且字符串为空字符串时不拼装
					if(stringMode || !(itemString.length()==0 && Character.isWhitespace(currentChar))) {
						itemString.append(currentChar);
					}
				}

				//处理数据
				if(!stringMode) {
					//JSON对象字符串分组,取 Key 对象,当前字符是:或=,则取 Key
					if (currentChar == Global.CHAR_COLON || currentChar == Global.CHAR_EQUAL) {
						itemString.setLength(itemString.length()-1);

						//itemString 尾部进行 trim 处理
						while(itemString.length()>0) {
							if(Character.isWhitespace(itemString.charAt(itemString.length() - 1))){
								itemString.setLength(itemString.length() - 1);
							} else {
								break;
							}
						}

						//判断是字符串去掉头尾的包裹符号
						if (itemString.length() >= 2 && itemString.charAt(0) == Global.CHAR_QUOTE && itemString.charAt(itemString.length()-1) == Global.CHAR_QUOTE) {
							key = itemString.substring(1, itemString.length() - 1).trim();
						}
						//判断是字符串去掉头尾的包裹符号
						else if (itemString.length() >= 2 && itemString.charAt(0) == Global.CHAR_S_QUOTE && itemString.charAt(itemString.length()-1) == Global.CHAR_S_QUOTE) {
							key = itemString.substring(1, itemString.length() - 1).trim();
						} else {
							key = itemString.substring(0).trim();
						}

						itemString.setLength(0);
					}

					//JSON对象字符串分组,取 value 对象,当前字符是,则取 value
					if (currentChar == Global.CHAR_COMMA) {
						if (value == null) {
							value = itemString.substring(0, itemString.length() - 1).trim();
						}
						itemString.setLength(0);
					}

					//JSON对象字符串分组,取 value 对象,当前字符是换行, 则取 value
					if (currentChar == '\n' || nextChar == 65535) {
						if (value == null && itemString.length() > 0) {
							value = itemString.substring(0).trim();
						}
						itemString.setLength(0);
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
						else if (TString.regexMatch(stringValue, "\\d\\.\\d*?E[+-]\\d+$")>0) {
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
						}
						//null值处理
						else if (value.equals("null")) {
							value = null;
						}
						//空值处理
						else if(value.equals("")){
							value = null;
						}


						if(value instanceof String) {
							//######  插值处理  #######
							if(enableToken) {
								value = tokenReplace(value.toString(), root, parentPath);
							}

							//######  URL引入处理  #######
							if(enablbeRef) {
								try {
									String tmpPath = key ==null ? parentPath : (parentPath.isEmpty()? key : parentPath+"."+key);
									value = include(value.toString(), enableToken, enablbeRef, tmpPath, root);
								} catch (Exception e) {
									throw new ParseException("Load JSON reference failed, "+key+":"+value+" not found", e);
								}
							}
						}

					}

					//====================  第二处 创建根对象(无根包裹)  ====================
					if(root == null) {
						if(key!=null) {
							if(parentKey!=null && parentRoot instanceof Map) {
								root = ((Map)parentRoot).get(parentKey);
							}
						}

						if(root==null) {
							if (key != null) {
								root = (Map) new LinkedHashMap<String, Object>(1024);
							} else {
								root = (List) new ArrayList<Object>(1024);
							}
						}

						//增加回写, 将数据回写至上层
						if(parentRoot instanceof List) {
							((List) parentRoot).add(root);
						}

						if(parentRoot instanceof Map && parentKey!=null) {
							((Map) parentRoot).put(parentKey, root);
						}
					}

					//判断返回对象的类型,填充返回对象
					if (root instanceof HashMap && key!=null && !filledByRecursion) {
						((Map) root).put(key, value);
					} else if (root instanceof ArrayList && value != null && !filledByRecursion) {
						((List) root).add(value);
						filledByRecursion = false;
					} else if(root == null){
						root = value;
					}
					
					filledByRecursion = false;
					//处理完侯将 value 放空
					key = null;
					value = null;
				}

				//纯字符串处理
				if (nextChar == 65535) {
					if(root==null && value == null && key == null) {
						root = itemString.toString();
					}
					break;
				}
			}

			return root;
		} catch(Exception e){
			try {
				int position = ((int) TReflect.getFieldValue(reader, "next"));
				String jsonStr = (String) TReflect.getFieldValue(reader, "str");
				int end = position+100 ;
				end = end > jsonStr.length()-1 ? jsonStr.length() : end;

				jsonStr = "code slice: {"+jsonStr.substring(0, position)+"\033[31m{<-ERROR}\033[0m"+jsonStr.substring(position, end) + "}\r\n";

				Logger.error(jsonStr, e);
			} catch (ReflectiveOperationException ex) {
				Logger.error(ex);
			}

			return null;

		}
	}


	/**
	 * 插值替换
	 * 	<li>通过".."操作引用上一级</li>
	 * @param value 字符串
	 * @param data 当前已解析的数据
	 * @return
	 */
	private static String tokenReplace(String value, Object data, String path) throws ParseException {

		int empthMarkLength = 2;
		//找到所有的插值替换标记
		String[] markArr = TString.searchByRegex(value, "\\|(.*?)\\|");

		if(markArr.length==0) {
			return value;
		}

		BeanVisitor loaclVisitor = BeanVisitor.newInstance(data);
		BeanVisitor rootVisitor = BeanVisitor.newInstance(ROOT.get());
		loaclVisitor.setPathSplitor(BeanVisitor.SplitChar.POINT);
		rootVisitor.setPathSplitor(BeanVisitor.SplitChar.POINT);


		List<String> marks = List.of(markArr);
		List<String> tokens= List.of(markArr).stream()
				.filter(mark->mark.length()>empthMarkLength)
				.map(mark->mark.substring(1, mark.length()-1))
				.collect(Collectors.toList());

		//插值替换
		for(int i=0; i<marks.size(); i++) {
			String mark = marks.get(i);
			String token = tokens.get(i);

			BeanVisitor visitor = rootVisitor;

			if(token.startsWith(".")) {
				token = TString.removePrefix(token);
				visitor = loaclVisitor;
			}

			if(token.startsWith("^")) {
				token = path  + "." + token;
				visitor = rootVisitor;
			}

			Object pathValue = visitor.value(token);

			if(pathValue!=null) {
				value = value.replace(mark, JSON.toJSON(pathValue));
			} else {
				throw new ParseException("Replace value " + value + " -> |"+ token + "| failed, target data is null");
			}
		}

		return value;
	}


	/**
	 * 解析引入URL内容处理方法
	 * @param value 当前引入URL的值
	 * @param enableToken 开启插值功能
	 * @param enablbeRef 开启引用功能
	 * @param path 挡墙的 key 字符串
	 * @param root 上级的 root 节点
	 * @return 文件解析后的对象, Map 或者 List
	 * @throws IOException IO 异常
	 */
	private static Object include( String value, boolean enableToken, boolean enablbeRef, String path, Object root) throws IOException {
		String url = null;
		Object ret = value;
		//引用文件处理
		if (value != null && value.charAt(0) == '@') {
			url = value.substring(1, value.length()).trim();
			url = CONTEXT_PATH.get().replace("{path}", url);
		} else if (value != null && value.charAt(0) == '#') {
			url = value.substring(1, value.length()).trim();
			if (TString.regexMatch(url, "^[a-z,A-Z]*?://") == 0) {
				url = "file://" + url;
			}
		} else {
			return value;
		}

		if(url!=null) {
			ret = parse(new URL(url), enableToken, enablbeRef,  path, root);
		}
		return ret;
	}

	/**
	 * 解析 JSON 字符串成为参数指定的类
	 * @param <T>         范型
	 * @param jsonStr    JSON字符串
	 * @param type        JSON 字符串将要转换的目标类
	 * @param ignoreCase 是否在字段匹配时忽略大小写
	 * @param enableToken 开启插值功能
	 * @param enablbeRef 开启引用功能
	 * @return                    JSON 转换后的 Java 对象
	 * @throws ReflectiveOperationException  反射异常
	 * @throws ParseException 解析异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T toObject(String jsonStr, Type type, boolean ignoreCase, boolean enableToken, boolean enablbeRef) throws ReflectiveOperationException {
		if(jsonStr==null){
			return null;
		}


		Object parseObject = parse(jsonStr, enableToken, enablbeRef);

		if(parseObject == null){
			parseObject = jsonStr;
		}

		try {
			if (type == Map.class && parseObject instanceof Map) {
				return (T) parseObject;
			}

			//{}包裹的对象处理
			else if (parseObject instanceof Map) {
				Map<String, Object> mapJSON = (Map<String, Object>) parseObject;
				return (T) TReflect.getObjectFromMap(type, mapJSON, ignoreCase);
			}
			//[]包裹的对象处理
			else if (parseObject instanceof Collection) {
				return (T) TReflect.getObjectFromMap(type, TObject.asMap(TReflect.SINGLE_VALUE_KEY, parseObject), false);
			}
			//如果传入的是标准类型则尝试用TString.toObject进行转换
			else if (parseObject instanceof String || parseObject.getClass().isPrimitive()) {
				return TString.toObject(parseObject.toString(), type);
			}
			//其他类型处理
			else {
				return null;
			}
		} catch (Exception e) {
			throw new ParseException(e);
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
	public static <T>T toObject(String jsonStr, Type type, boolean ignoreCase) throws ReflectiveOperationException, ParseException {
		return toObject(jsonStr, type, ignoreCase, false, false);

	}

	/**
	 * 解析 JSON 字符串成为参数指定的类,默认严格限制字段大小写
	 * @param <T>         范型
	 * @param jsonStr    JSON字符串
	 * @param clazz        JSON 字符串将要转换的目标类
	 * @return                    JSON 转换后的 Java 对象
	 * @throws ReflectiveOperationException  反射异常
	 * @throws ParseException 解析异常
	 * @throws IOException IO 异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T toObject(String jsonStr, Class<T> clazz) throws ParseException, ReflectiveOperationException, IOException {
		return toObject(jsonStr, clazz, false, false, false);
	}
}
