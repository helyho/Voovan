package org.hocate.log;

import java.io.File;

import org.hocate.tools.TFile;
import org.hocate.tools.TProperties;

public class StaticParam {
	private static long		startTimeMillis	= System.currentTimeMillis();
	private static File		configFile		= TFile.getResourceFile("logger.properties");

	public static long getStartTimeMillis() {
		return startTimeMillis;
	}
	
	public static String getLogConfig(String property) {
		String value = TProperties.getString(configFile, property);
		return value==null?"":value;
	}
}
