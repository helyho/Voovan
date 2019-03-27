package org.voovan.tools.json;

import org.voovan.tools.TFile;
import org.voovan.tools.TString;

import java.io.IOException;

/**
 * JSON字符串分析成 Map
 *
 * @author helyho
 * <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class YAML2JSON {
	public static ThreadLocal<Integer> YAML_READ_INDEX = ThreadLocal.withInitial(()->0);

	/**
	 * 转换 YAML 到 JSON
	 * 如果是{}包裹的对象解析成 HashMap,如果是[]包裹的对象解析成 ArrayList
	 *
	 * @param jsonStr 待解析的 JSON 字符串
	 * @return 解析后的对象
	 * @throws IOException IO异常
	 */
	public static String convert(String jsonStr) throws IOException {
		YAML_READ_INDEX.set(0);

		StringBuilder result = new StringBuilder();

		int firstLineType = -1;

		while (true) {

			int lineType = getNextLineType(jsonStr);

			if (lineType == -1) {
				break;
			}

			if(firstLineType == -1){
				firstLineType = lineType;
			}

			result.append(parse(lineType, jsonStr));
		}


		if (result.length()>0 && result.charAt(result.length() - 1) == ',') {
			result.deleteCharAt(result.length() - 1);
		}

		//如果是键值/多行字符串类型补充前后符号
		if(firstLineType == 1 || firstLineType == 2){
			result.insert(0, '{');
			result.append('}');
		}

		return result.toString();
	}

	/**
	 * 获取下一行数据的类型
	 *
	 * @param jsonStr json字符串
	 * @return 0: array, 1: map, 2: 多行文本, -1: end of buffer
	 */
	public static int getNextLineType(String jsonStr) {
		String line = readLine(jsonStr);
		if (line != null) {
			writeBack(line);
			line = line.trim();
			if (line.endsWith(">") || line.endsWith("|") || line.endsWith("|+") || line.endsWith("|-")) {
				return 2;
			} else if (line.contains(":")) {
				return 1;
			} else if (line.startsWith("-")) {
				return 0;
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	/**
	 * 通用解析函数
	 * @param nextLintType 缓冲区中下一行数据的类型
	 * @param jsonStr 字节缓冲区
	 * @return 解析出的 JSON 字符串
	 */
	public static String parse(int nextLintType, String jsonStr) {
		String result = null;
		switch (nextLintType) {
			case 0:
				result = parseArray(jsonStr);
				break;
			case 1:
				result = parseMap(jsonStr);
				break;
			case 2:
				result = parseMulitLineString(jsonStr);
				break;
			default:
				break;
		}

		return result;
	}

	public static String readLine(String jsonStr){
		String line = TString.readLine(jsonStr, YAML_READ_INDEX.get());
		if(line!=null) {
			YAML_READ_INDEX.set(YAML_READ_INDEX.get() + line.length());
		}
		return line;
	}

	/**
	 * 解析数组信息
	 * @param jsonStr 字节缓冲区
	 * @return 解析出de JSON 字符串
	 */
	public static String parseArray(String jsonStr) {

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("[");

		int prevLineRetract = -1;

		while (true) {
			String line = readLine(jsonStr);

			if (line == null) {
				if (stringBuilder.length()>0 && stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
				}
				break;
			}

			if(line.trim().startsWith("#")){
				continue;
			}

			int lineRetract = TString.retract(line);
			String lineValue = TString.trimEndLF(line.substring(lineRetract == 0 ? 0 : lineRetract + 1));

			if (prevLineRetract == -1) {
				prevLineRetract = lineRetract;
			}

			//缩进改变
			if (prevLineRetract < lineRetract) {
				writeBack(line);

				if (stringBuilder.length()>0 && stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
				}

				stringBuilder.append("],");
				break;
			} else if(prevLineRetract > lineRetract){

				if (stringBuilder.length()>0 && stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
				}

				writeBack(line);
				break;
			}

			int nextLineType = getNextLineType(jsonStr);

			//键值 Map
			if(nextLineType == 1){
				stringBuilder.append("{"+parseMap(jsonStr)+"}");
			}
			//多行字符串
			else if (lineValue.endsWith(">") || lineValue.endsWith("|") ||
					lineValue.endsWith("|+") || lineValue.endsWith("|-")){
				writeBack(line);
				stringBuilder.append(parseMulitLineString(jsonStr)).append(",");
			}
			//数组元素
			else if (lineValue.startsWith("-")) {
				stringBuilder.append(formatLine(lineValue.replaceAll("- *", ""))).append(",");
			}
		}

		if (stringBuilder.length()>0 && stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
			stringBuilder.deleteCharAt(stringBuilder.length() - 1);
		}

		stringBuilder.append("]");

		return stringBuilder.toString();
	}

	/**
	 * 解析键值信息
	 * @param jsonStr 字节缓冲区
	 * @return 解析出JSON 字符串
	 */
	public static String parseMap(String jsonStr) {

		StringBuilder stringBuilder = new StringBuilder();

		int prevLineRetract = -1;

		while (true) {
			String line = readLine(jsonStr);

			if (line == null) {
				if (stringBuilder.length()>0 && stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
				}
				break;
			}

			if(line.trim().startsWith("#")){
				continue;
			}

			int lineRetract = TString.retract(line);
			String lineValue = TString.trimEndLF(line.substring(lineRetract == 0 ? 0 : lineRetract + 1));

			if (prevLineRetract == -1) {
				prevLineRetract = lineRetract;
			}

			int nextLineType = getNextLineType(jsonStr);

			//缩进改变
			if (prevLineRetract < lineRetract) {
				writeBack(line);

				lineValue = parse(nextLineType, jsonStr);
				stringBuilder.append(lineValue);

				if (stringBuilder.length()>0 && stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
				}

				stringBuilder.append("},");

				if(lineValue.startsWith("[") && lineValue.endsWith("]")){
					stringBuilder.deleteCharAt(stringBuilder.indexOf("{"));

					int closePostion = stringBuilder.lastIndexOf("},");
					stringBuilder.delete(closePostion, closePostion+2);
					stringBuilder.append(",");
				}
				break;
			} else if (prevLineRetract > lineRetract) {
				writeBack(line);
				if (stringBuilder.length()>0 && stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
					stringBuilder.deleteCharAt(stringBuilder.length() - 1);
				}
				break;
			}

			//数组
			if(nextLineType == 0){
				stringBuilder.append(formatLine(lineValue));
				stringBuilder.append(parse(nextLineType, jsonStr)).append(",");
			}
			//多行字符串
			else if (lineValue.trim().endsWith(">") || lineValue.trim().endsWith("|") ||
					lineValue.trim().endsWith("|+") || lineValue.trim().endsWith("|-")){
				writeBack(line);
				stringBuilder.append(parseMulitLineString(jsonStr)).append(",");
			} else if(lineValue.trim().endsWith(":")){
				stringBuilder.append(formatLine(lineValue)).append("{");
			} else if(lineValue.contains(": ") && !lineValue.trim().endsWith(":")){
				stringBuilder.append(formatLine(lineValue)).append(",");
			}
		}

		return stringBuilder.toString();
	}

	/**
	 * 回写数据
	 *
	 * @param line              需要回写的数据
	 */
	public static void writeBack(String line) {
		YAML_READ_INDEX.set(YAML_READ_INDEX.get() - line.length());
	}

	/**
	 * 将当前行格式化成 JSON 形式
	 *
	 * @param line 行字符串
	 * @return 当前行的 JSON 形式
	 */
	public static String formatLine(String line) {
		line = line.replace("- ", "");
		int pairIndex = line.indexOf(":");
		//键值对处理
		if (pairIndex > 0) {
			String key = line.substring(0, pairIndex).trim();
			String value = "";
			if (pairIndex <= line.length() - 2) {
				value = line.substring(pairIndex + 2).trim(); //实际上定位的是 ": "
			}

			if (!value.isEmpty() &&
					!value.startsWith("\"") && !value.endsWith("\"") &&
					!TString.isNumber(value, 10) && !TString.isDecimal(value)) {
				value = "\"" + value + "\"";
			}

			line = "\"" + key + "\":" + value;
		}
		//数组处理
		else {

			if (!TString.isNumber(line, 10) && !TString.isDecimal(line)) {
				line = "\"" + line + "\"";
			}
		}
		return line;
	}

	/**
	 * 处理多行字符串
	 *
	 * @param jsonStr 换红区
	 * @return 按规则合并后的字符串
	 */
	public static String parseMulitLineString(String jsonStr) {
		int initLineRetract = 0;
		int lineRetract = 0;
		String markLine = readLine(jsonStr);
		String line = null;
		String result = "";
		markLine = markLine.trim();
		while (true) {
			//处理多行字符
			line = readLine(jsonStr);

			if (line == null) {
				break;
			}

			if(line.trim().startsWith("#")){
				continue;
			}

			lineRetract = TString.retract(line);
			String lineValue = line.substring(lineRetract == 0 ? 0 : lineRetract + 1);
			if (lineRetract == initLineRetract || initLineRetract == 0) {
				result = result + lineValue;
				initLineRetract = lineRetract;
			} else {
				writeBack(line);
				break;
			}
		}

		//只保留尾部的最后一个换行符
		if (markLine.endsWith("-")) {
			result = TString.trimEndLF(result) + TFile.getLineSeparator();
		}

		//不保留任何换行符
		if (markLine.endsWith(">")) {
			result = result.replace(TFile.getLineSeparator(), "");
		}

		//仅不保留尾部的换行符
		if (markLine.endsWith("|")) {
			result = TString.trimEndLF(result);
		}

		markLine = TString.fastReplaceAll(markLine, ":\\s*(\\||>)[+-]?", ":");

		markLine = markLine + TString.convertEscapeChar(result);

		return formatLine(markLine)+",";
	}
}
