package org.voovan.tools;

import org.voovan.Global;

import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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

	public final static long SECOND = TimeUnit.SECONDS.toMillis(1);
	public final static long MINUTE = TimeUnit.MINUTES.toMillis(1);
	public final static long HOUR = TimeUnit.HOURS.toMillis(1);
	public final static long DAY = TimeUnit.DAYS.toMillis(1);

	public final static Long BOOT_TIME_MILLS = System.currentTimeMillis();
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
	 * 时间戳转换
	 * @param value 待转换的时间戳
	 * @param timeUnit 时间单位
	 * @param format 转换格式
	 * @return 转后的时间
	 */
	public static String timestamp(long value, TimeUnit timeUnit, String format) {
		return format(new Date(timeUnit.toMillis(value)), format);
	}

	/**
	 * 时间戳转换
	 * @param value 待转换的时间戳(毫秒)
	 * @param format 转换格式
	 * @return 转后的时间
	 */
	public static String timestamp(long value, String format) {
		return format(new Date(value), format);
	}

	/**
	 * 时间戳转换为标准时间格式
	 * @param value 待转换的时间戳(毫秒)
	 * @return 转后的时间
	 */
	public static String timestamp(long value) {
		return format(new Date(value));
	}


	/**
	 * 格式化日期成字符串
	 * @param date Date 对象
	 * @return 日期字符串
	 */
	public static String format(Date date){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(STANDER_DATETIME_TEMPLATE);
		return simpleDateFormat.format(date);
	}

	/**
	 * 格式化日期成字符串
	 * @param date Date 对象
	 * @param format 日期格式模板
	 * @return 日期字符串
	 */
	public static String format(Date date,String format){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormat.format(date);
	}

	/**
	 * 格式化日期成字符串
	 * @param date 		Date 对象
	 * @param format 	日期格式模板
	 * @param timeZone	所在时区
	 * @return 日期字符串
	 */
	public static String format(Date date,String format,String timeZone){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
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
	public static String format(Date date,String format,String timeZone,Locale local){
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
	 * @return Date 对象
	 */
	public static Date parse(String source) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(STANDER_DATETIME_TEMPLATE);
		return simpleDateFormatParse(simpleDateFormat, source);
	}

	/**
	 * 从字符串解析时间
	 * @param source 日期字符串
	 * @param format 日期格式模板
	 * @return Date 对象
	 */
	public static Date parse(String source,String format) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		return simpleDateFormatParse(simpleDateFormat, source);
	}

	/**
	 * 从字符串解析时间
	 * @param source 	日期字符串
	 * @param format 	日期格式模板
	 * @param timeZone	所在时区
	 * @return Date 对象
	 */
	public static Date parse(String source,String format,String timeZone) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormatParse(simpleDateFormat, source);
	}

	/**
	 * 按照特定的时区和地狱从字符串解析时间
	 * @param source		日期字符串
	 * @param format		日志格式化字符串
	 * @param timeZone		所在时区
	 * @param local			所在区域
	 * @return 日期字符串
	 */
	public static Date parse(String source,String format,String timeZone,Locale local) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, local);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormatParse(simpleDateFormat, source);
	}

	private static Date simpleDateFormatParse(SimpleDateFormat simpleDateFormat, String source){
		try{
			return simpleDateFormat.parse(source);
		} catch (ParseException e){
			throw new org.voovan.tools.exception.ParseException(e);
		}
	}

	/**
	 * 按格林威治时间(GMT)时间格式获取时间对象
	 * @param source 日期字符串
	 * @return Date 对象
	 */
	public static Date parseToGMT(String source) {
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
	public static String add(String time,long millis,String format) throws ParseException{
		Date tmpDate = parse(time, format);
		Date resultDate = add(tmpDate, millis);
		return format(resultDate, format);
	}

	/**
	 * 获取日期中的时间元素
	 * @param date 日期对象
	 * @param type 时间元素类型, 例如: Calendar.MINUTE
	 * @return 时间元素的值
	 */
	public static int getDateElement(Date date,int type){
		Calendar calendar = Calendar.getInstance();
		if(date!=null) {
			calendar.setTime(date);
		}
		return calendar.get(type);
	}

	/**
	 * 用来获取纳
	 * 		不可用来做精确计时,只能用来做时间标记
	 * @return 当前的纳秒时间
	 */
	public static Long currentTimeNanos(){
		return BOOT_TIME_MILLS*1000000 + System.nanoTime();
	}

	/**
	 * 格式一个差值时间为人类可读
	 * 	如:3600秒, 格式化为: 1h
	 *
	 * @param secs 时间秒
	 * @return 格式化后的时间
	 */
	public static String formatElapsedSecs(long secs) {
		long eTime = secs;
		final long days = TimeUnit.SECONDS.toDays(eTime);
		eTime -= TimeUnit.DAYS.toSeconds(days);
		final long hr = TimeUnit.SECONDS.toHours(eTime);
		eTime -= TimeUnit.HOURS.toSeconds(hr);
		final long min = TimeUnit.SECONDS.toMinutes(eTime);
		eTime -= TimeUnit.MINUTES.toSeconds(min);
		final long sec = eTime;

		if(days == 0) {
			return String.format("%02d:%02d:%02d", hr, min, sec);
		} else {
			return String.format("%d, %02d:%02d:%02d", days, hr, min, sec);
		}
	}
}
