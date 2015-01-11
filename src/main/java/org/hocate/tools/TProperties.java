package org.hocate.tools;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * properties文件操作类
 * @author helyho
 *
 */
public class TProperties {

	public static Properties getProperties(String fileName){
		Properties properites = new Properties();
		try {
			properites.load(new FileReader(fileName));
			return properites;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getString(String fileName,String name){
		Properties properites = getProperties(fileName);
		return TObject.nullDefault(properites.getProperty(name),null);
	}
	
	public static int getInt(String fileName,String name){
		String value = getString(fileName,name);
		return TObject.nullDefault(Integer.valueOf(value),0);
	}
	
	public static float getFloat(String fileName,String name){
		String value = getString(fileName,name);
		return TObject.nullDefault(Float.valueOf(value.trim()),0).floatValue();
	}
	
	public static double getDouble(String fileName,String name){
		String value = getString(fileName,name);
		return TObject.nullDefault(Double.valueOf(value.trim()),0).doubleValue();
	}
	
}
