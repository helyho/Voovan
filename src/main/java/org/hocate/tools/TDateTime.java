package org.hocate.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class TDateTime {
	/**
	 * 获取当前时间
	 * @return
	 */
	public static String currentTime(){
		return dateFormat(new Date(),"YYYY-MM-dd HH:mm:ss");
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
	public static String dateFormat(Date date,String format){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.format(date);
	}
	
	/**
	 * 使用特定时区,格式化日期成字符串
	 * @param date			日期对象
	 * @param format		日期格式化字符串
	 * @param timeZone		所在时区
	 * @param loacl			所在区域
	 * @return
	 */
	public static String dateFormat(Date date,String format,String timeZone,Locale loacl){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format,loacl);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormat.format(date);
	}
	
	/**
	 * 获取标准的格林威治时间(GMT)
	 * @param date
	 * @return
	 */
	public static String formatStanderGMTDate(Date date){
		return dateFormat(date, "EEE, d MMM yyyy HH:mm:ss 'GMT'", "GMT",Locale.ENGLISH);
	}	
	
	/**
	 * 从字符串解析时间
	 * @param source
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDate(String source,String format) throws ParseException{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.parse(source);
	}
	
	/**
	 * 从字符串解析时间
	 * @param source		日期字符串
	 * @param format		日志格式化字符串
	 * @param timeZone		所在时区
	 * @param loacl			所在区域
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDate(String source,String format,String timeZone,Locale loacl) throws ParseException{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format,loacl);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormat.parse(source);
	}
	
	/**
	 * 从字符串获取标准的格林威治时间(GMT)
	 * @param source
	 * @return
	 * @throws ParseException 
	 */
	public static Date parseStanderGMTDate(String source) throws ParseException{
		return parseDate(source, "EEE, d MMM yyyy HH:mm:ss 'GMT'", "GMT",Locale.ENGLISH);
	}	
}
