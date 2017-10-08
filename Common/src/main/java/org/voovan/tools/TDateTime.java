package org.voovan.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
	public final static String INTERNTTION_DATETIME_TEMPLATE = "EEE, dd MMM yyyy HH:mm:ss z";
	
	/**
	 * 获取当前时间
	 * 		yyyy-MM-dd HH:mm:ss
	 * @return 日期字符串
	 */
	public static String now(){
		return format(new Date(),STANDER_DATETIME_TEMPLATE);
	}
	
	/**
	 * 根据特定格式获取当前时间
	 * @param format 日期格式模板
	 * @return 日期字符串
	 */
	public static String now(String format){
		return format(new Date(),format);
	}
	
	/**
	 * 格式化日期成字符串
	 * @param date Date 对象
	 * @param format 日期格式模板
	 * @return 日期字符串
	 */
	public static String format(Date date, String format){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.format(date);
	}

	/**
	 * 格式化日期成字符串
	 * @param date Date 对象
	 * @param format 日期格式模板
	 * @param local 所在区域
	 * @return 日期字符串
	 */
	public static String format(Date date, String format, Locale local){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format,local);
		return simpleDateFormat.format(date);
	}

	/**
	 * 使用特定时区,格式化日期成字符串
	 * @param date			日期对象
	 * @param format		日期格式化字符串
	 * @param timeZone		所在时区
	 * @param local			所在区域
	 * @return 日期字符串
	 */
	public static String format(Date date, String format, String timeZone, Locale local){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format,local);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormat.format(date);
	}
	
	/**
	 * 获取标准的格林威治时间(GMT)
	 * @param date Date 对象
	 * @return 日期字符串
	 */
	public static String formatToGMT(Date date){
		return format(date, INTERNTTION_DATETIME_TEMPLATE, "GMT", Locale.ENGLISH);
	}

	/**
	 * 从字符串解析时间
	 * @param source 日期字符串
	 * @param format 日期格式模板
	 * @return Date 对象
	 * @throws ParseException 解析异常
	 */
	public static Date parse(String source, String format) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.parse(source);
	}

	/**
	 * 从字符串解析时间
	 * @param source 日期字符串
	 * @param format 日期格式模板
	 * @param local  所在区域
	 * @return Date 对象
	 * @throws ParseException 解析异常
	 */
	public static Date parse(String source, String format, Locale local) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.parse(source);
	}
	
	/**
	 * 按照特定的时区和地狱从字符串解析时间
	 * @param source		日期字符串
	 * @param format		日志格式化字符串
	 * @param timeZone		所在时区
	 * @param local			所在区域
	 * @return 日期字符串
	 * @throws ParseException 解析异常
	 */
	public static Date parse(String source, String format, String timeZone, Locale local) throws ParseException {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, local);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormat.parse(source);
	}
	
	/**
	 * 按格林威治时间(GMT)时间格式获取时间对象
	 * @param source 日期字符串
	 * @return Date 对象
	 * @throws ParseException 解析异常
	 */
	public static Date parseToGMT(String source) throws ParseException {
		return parse(source, INTERNTTION_DATETIME_TEMPLATE, "GMT", Locale.ENGLISH);
	}

	/**
	 * 日期加操作
	 * @param date		加法的基数日期
	 * @param millis	微秒
	 * @return Date 对象
	 */
	public static Date add(Date date, long millis){
		 return new Date(date.getTime()+millis);
	}
	
	/**
	 * 日期加操作
	 * @param time		加法的基数日期
	 * @param millis	微秒
	 * @param format    输出的格式
	 * @return 日期字符串
	 * @throws ParseException  解析异常
	 */
	public static String add(String time, long millis, String format) throws ParseException {
		Date tmpDate = parse(time, format);
		Date resultDate = add(tmpDate, millis);
		return format(resultDate, format);
	}

	/**
	 * 获取日期中的时间元素
	 * @param date 日期对象
	 * @param type 时间元素类型
	 */
	public static int getDateAtom(Date date, int type){
		Calendar calendar = Calendar.getInstance();
		if(date!=null) {
			calendar.setTime(date);
		}
		return calendar.get(type);
	}
}
