package org.voovan.tools.log;

import java.io.File;

import org.voovan.tools.TFile;
import org.voovan.tools.TProperties;

public class StaticParam {
	private static long		startTimeMillis	= System.currentTimeMillis();
	private static File		configFile		= loadConfig();

	public static File loadConfig(){
		File tmpFile = TFile.getResourceFile("logger.properties");
		if(tmpFile!=null){
			return tmpFile;
		}else{
			System.out.println("Log util Waring: Can't found log config file!");
			return null;
		}
	}
	
	public static long getStartTimeMillis() {
		return startTimeMillis;
	}
	
	public static String getLogConfig(String property,String defalut) {
		String value = null;
		if(configFile!=null){
			value = TProperties.getString(configFile, property);
		}
		return value==null?defalut:value;
	}
}
