package org.voovan.tools.log;

import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.TString;
import static org.voovan.tools.TString.TOKEN_PREFIX_REGEX;
import static org.voovan.tools.TString.TOKEN_SUFFIX_REGEX;
import static org.voovan.tools.TString.TOKEN_PREFIX;
import static org.voovan.tools.TString.TOKEN_SUFFIX;

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
	public final static String LOG_LEVEL = "ALL";
	public final static String LOG_FILE = "<WorkDir>/logs/sysout.<D>.log";
	public final static String LOG_TYPE = "STDOUT";
	public static String DEFAULT_LOG_TEMPLATE =
			"--------------------------------------------------------------------------------------------------------------------------------------------------" +
			"<n>[<P>] [<D>] [Thread:<T>] [Time:<R>] (<F>:<L>) <n>" +
			"--------------------------------------------------------------------------------------------------------------------------------------------------" +
			"<n><I><n><n>";
	static {
		DEFAULT_LOG_TEMPLATE = DEFAULT_LOG_TEMPLATE.replace("<",TOKEN_PREFIX).replace(">",TOKEN_SUFFIX);
	}

	public final static String LOG_TEMPLATE = getLogConfig("LogTemplate", LoggerStatic.DEFAULT_LOG_TEMPLATE);

	public static boolean HAS_COLOR = 	TString.regexMatch(LOG_TEMPLATE, TOKEN_PREFIX_REGEX + "[FB][0-7D]"	+ TOKEN_SUFFIX_REGEX) > 0;
	public static boolean HAS_STACK = 	TString.regexMatch(LOG_TEMPLATE, TOKEN_PREFIX_REGEX + "SI"	+ TOKEN_SUFFIX_REGEX) > 0 ||
										TString.regexMatch(LOG_TEMPLATE, TOKEN_PREFIX_REGEX + "L" 	+ TOKEN_SUFFIX_REGEX) > 0 ||
										TString.regexMatch(LOG_TEMPLATE, TOKEN_PREFIX_REGEX + "M" 	+ TOKEN_SUFFIX_REGEX) > 0 ||
										TString.regexMatch(LOG_TEMPLATE, TOKEN_PREFIX_REGEX + "F" 	+ TOKEN_SUFFIX_REGEX) > 0 ||
										TString.regexMatch(LOG_TEMPLATE, TOKEN_PREFIX_REGEX + "C" 	+ TOKEN_SUFFIX_REGEX) > 0;

						public static boolean HAS_LEVEL 	= TString.regexMatch(LOG_TEMPLATE, 	TOKEN_PREFIX_REGEX+"P"+TOKEN_SUFFIX_REGEX) > 0;
	public static boolean HAS_DATE 		= TString.regexMatch(LOG_TEMPLATE, 	TOKEN_PREFIX_REGEX + "D" + TOKEN_SUFFIX_REGEX) > 0;
	public static boolean HAS_THREAD 	= TString.regexMatch(LOG_TEMPLATE, 	TOKEN_PREFIX_REGEX + "T" + TOKEN_SUFFIX_REGEX) > 0;
	public static boolean HAS_RUNTIME 	= TString.regexMatch(LOG_TEMPLATE, 	TOKEN_PREFIX_REGEX + "R" + TOKEN_SUFFIX_REGEX) > 0;
	public static boolean JSON_FORMAT  	= Boolean.valueOf(getLogConfig("JsonFormat", "true"));

	/**
	 * 获取日志配置项信息
	 * @param property  日志配置项
	 * @param defalut   默认值
	 * @return  日志配置信息
	 */
	protected static String getLogConfig(String property, String defalut) {
		return TProperties.getString("logger", property, defalut);
	}
}
