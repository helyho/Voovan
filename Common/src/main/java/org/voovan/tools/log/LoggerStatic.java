package org.voovan.tools.log;

import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;

import java.io.File;

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
	public final static String LOG_TEMPLATE = "--------------------------------------------------------------------------------------------------------------------------------------------------" +
			"{{n}}[{{P}}] [{{D}}] [Thread:{{T}}] [Time:{{R}}] ({{F}}:{{L}}) {{n}}" +
			"--------------------------------------------------------------------------------------------------------------------------------------------------" +
			"{{n}}{{I}}{{n}}{{n}}";
	public final static String LINE_HEAD = "";
	public final static String LINE_TAIL = "";
	public final static String LINE_ALIGN_LEFT = "";
	public final static String MAX_LINE_LENGTH = "-1";

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
