package org.hocate.tools;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * properties文件操作类
 * @author helyho
 *
 */
public class TProperties {

	public static Properties getProperties(File file){
		Properties properites = new Properties();
		try {
			properites.load(new FileReader(file));
			return properites;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static String getString(File file,String name){
		Properties properites = getProperties(file);
		return TObject.nullDefault(properites.getProperty(name),null);
	}
	
	public static int getInt(File file,String name){
		String value = getString(file,name);
		return TObject.nullDefault(Integer.valueOf(value),0);
	}
	
	public static float getFloat(File file,String name){
		String value = getString(file,name);
		return TObject.nullDefault(Float.valueOf(value.trim()),0).floatValue();
	}
	
	public static double getDouble(File file,String name){
		String value = getString(file,name);
		return TObject.nullDefault(Double.valueOf(value.trim()),0).doubleValue();
	}
	
}
