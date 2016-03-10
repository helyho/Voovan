package org.voovan.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 时间工具类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */


public class TDateTime {
	public final static String STANDER_DATE_TEMPLATE = "yyyy-MM-dd";
	public final static String STANDER_TIME_TEMPLATE = "HH:mm:ss";
	public final static String STANDER_DATETIME_TEMPLATE = "yyyy-MM-dd HH:mm:ss";
	public final static String GMT_DATETIME_TEMPLATE = "EEE, d MMM yyyy HH:mm:ss 'GMT'";
	
	/**
	 * 获取当前时间
	 * 		yyyy-MM-dd HH:mm:ss
	 * @return
	 */
	public static String now(){
		return format(new Date(),STANDER_DATETIME_TEMPLATE);
	}
	
	/**
	 * 根据特定格式获取当前时间
	 * @param format
	 * @return
	 */
	public static String now(String format){
		return format(new Date(),format);
	}
	
	/**
	 * 格式化日期成字符串
	 * @param date
	 * @param format
	 * @return
	 */
	public static String format(Date date,String format){
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
	public static String format(Date date,String format,String timeZone,Locale loacl){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format,loacl);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormat.format(date);
	}
	
	/**
	 * 获取标准的格林威治时间(GMT)
	 * @param date
	 * @return
	 */
	public static String formatToGMT(Date date){
		return format(date, GMT_DATETIME_TEMPLATE, "GMT",Locale.ENGLISH);
	}	
	
	/**
	 * 从字符串解析时间
	 * @param source
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	public static Date parse(String source,String format) throws ParseException{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.parse(source);
	}
	
	/**
	 * 按照特定的时区和地狱从字符串解析时间
	 * @param source		日期字符串
	 * @param format		日志格式化字符串
	 * @param timeZone		所在时区
	 * @param loacl			所在区域
	 * @return
	 * @throws ParseException
	 */
	public static Date parse(String source,String format,String timeZone,Locale loacl) throws ParseException{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format,loacl);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormat.parse(source);
	}
	
	/**
	 * 按格林威治时间(GMT)时间格式获取事件对象
	 * @param source
	 * @return
	 * @throws ParseException 
	 */
	public static Date parseToGMT(String source) throws ParseException{
		return parse(source, GMT_DATETIME_TEMPLATE, "GMT",Locale.ENGLISH);
	}	

	/**
	 * 日期加操作
	 * @param date		加法的基数日期
	 * @param millis	微秒
	 * @return
	 */
	public static Date add(Date date,long millis){
		 return new Date(date.getTime()+millis);
	}
	
	/**
	 * 日期加操作
	 * @param date		加法的基数日期
	 * @param millis	微秒
	 * @param format    输出的格式
	 * @return
	 * @throws ParseException 
	 */
	public static String add(String time,long millis,String format) throws ParseException{
		Date tmpDate = parse(time, format);
		Date resultDate = add(tmpDate, millis);
		return format(resultDate, format);
	}


}
