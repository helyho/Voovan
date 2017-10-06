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
public class StaticParam {
	private static long		startTimeMillis	= System.currentTimeMillis();
	private static File		configFile		= getConfigFile();

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
	 * 读取日志配置文件信息
	 * @return 日志配置文件对象
	 */
	protected static File getConfigFile(){
		File tmpFile =  tmpFile = new File("./classes/logger.properties");

		//如果从 classes 目录中找不到,则从 classpath 中寻找
		if(tmpFile==null || !tmpFile.exists()){
			tmpFile = TFile.getResourceFile("logger.properties");
		}

		if(tmpFile!=null){
			return tmpFile;
		}else{
			System.out.println("Log util Waring: Can't found log config file!");
			System.out.println("Log util Waring: System will be use default config: LogType just STDOUT!");
			return null;
		}
	}

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
	protected static String getLogConfig(String property,String defalut) {
		String value = null;
		if(configFile!=null){
			value = TProperties.getString(configFile, property);
		}
		return TObject.nullDefault(value,defalut);
	}
}
