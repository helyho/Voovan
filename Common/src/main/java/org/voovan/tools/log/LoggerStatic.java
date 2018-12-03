package org.voovan.tools.log;

import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.TString;

/**
 * 静态对象类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class LoggerStatic {
	private static long		startTimeMillis	= System.currentTimeMillis();

	public final static String LOG_LEVEL = "ALL";
	public final static String LOG_FILE = null;
	public final static String LOG_TYPE = "STDOUT";
	public final static String DEFAULT_LOG_TEMPLATE = "--------------------------------------------------------------------------------------------------------------------------------------------------" +
			"{{n}}[{{P}}] [{{D}}] [Thread:{{T}}] [Time:{{R}}] ({{F}}:{{L}}) {{n}}" +
			"--------------------------------------------------------------------------------------------------------------------------------------------------" +
			"{{n}}{{I}}{{n}}{{n}}";
	public final static String LOG_TEMPLATE = LoggerStatic.getLogConfig("LogTemplate", LoggerStatic.DEFAULT_LOG_TEMPLATE);

	public static boolean HAS_COLOR = TString.regexMatch(LOG_TEMPLATE, "\\{\\{[FB][0-7D]\\}\\}") > 0;
	public static boolean HAS_STACK = TString.regexMatch(LOG_TEMPLATE, "\\{\\{SI\\}\\}") > 0 || TString.regexMatch(LOG_TEMPLATE, "\\{\\{L\\}\\}") > 0 ||
			TString.regexMatch(LOG_TEMPLATE, "\\{\\{M\\}\\}") > 0 ||
			TString.regexMatch(LOG_TEMPLATE, "\\{\\{F\\}\\}") > 0 ||
			TString.regexMatch(LOG_TEMPLATE, "\\{\\{C\\}\\}") > 0;

	public static boolean HAS_LEVEL = TString.regexMatch(LOG_TEMPLATE, "\\{\\{P\\}\\}") > 0;
	public static boolean HAS_DATE = TString.regexMatch(LOG_TEMPLATE, "\\{\\{D\\}\\}") > 0;
	public static boolean HAS_THREAD = TString.regexMatch(LOG_TEMPLATE, "\\{\\{T\\}\\}") > 0;
	public static boolean HAS_RUNTIME = TString.regexMatch(LOG_TEMPLATE, "\\{\\{R\\}\\}") > 0;

	/**
	 * 获取启动时间信息
	 * @return 启动时间
	 */
	protected static long getStartTimeMillis() {
		return startTimeMillis;
	}

	/**
	 * 获取日志配置项信息
	 * @param property  日志配置项
	 * @param defalut   默认值
	 * @return  日志配置信息
	 */
	protected static String getLogConfig(String property, String defalut) {
		String value = TProperties.getString("logger", property);
		return TObject.nullDefault(value,defalut);
	}
}
