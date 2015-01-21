package org.hocate.tools;

import java.text.SimpleDateFormat;
import java.util.Date;


public class TDateTime {
	/**
	 * 获取当前时间
	 * @return
	 */
	public static String currentTime(){
		return dateFormat(new Date(),"YYYY-MM-DD HH:mm:ss");
	}
	
	/**
	 * 根据特定格式获取当前时间
	 * @param format
	 * @return
	 */
	public static String currentTime(String format){
		return dateFormat(new Date(),format);
	}
	
	/**
	 * 格式化日期成字符串
	 * @param date
	 * @param format
	 * @return
	 */
	private static String dateFormat(Date date,String format){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.format(date);
	}
}
