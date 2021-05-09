package org.voovan.tools.log;

import org.voovan.tools.*;
import org.voovan.tools.json.JSON;

import java.util.function.Function;

/**
 * 日志工具类
 *
 * @author helyho
 *
 *         Voovan Framework. WebSite: https://github.com/helyho/Voovan Licence:
 *         Apache v2 License
 */
public class Logger {
	private static Formater	formater	= Formater.newInstance();
	private static boolean enable 		= true;

	/**
	 * 日志输出状态
	 *
	 * @return true:输出日志,false 不输出任何日志
	 */
	public static boolean isEnable() {
		return enable;
	}

	/**
	 * 设置日志输出状态
	 * @param enable true:输出日志,false 不输出任何日志
	 */
	public static void setEnable(boolean enable) {
		Logger.enable = enable;
	}

	public static void stopLoggerThread(){

	}

	/**
	 * 判断是否包含指定的日志级别
	 * @param logLevel  日志级别
	 * @return true: 包含, false: 不包含
	 */
	public static boolean hasLevel(String logLevel){
		if(Formater.ALL==0){
			return true;
		}

		if(formater.getLogLevel().contains(logLevel)){
			return true;
		}else{
			return false;
		}
	}


	//============================================== INFO ==============================================
	public static void info(Object msg) {
		basicLog(Formater.INFO, msg);
	}

	public static void infof(String msg, Object ... args){
		basicLog(Formater.INFO, msg, null, args);
	}

	//============================================== FRAMEWORK ==============================================
	public static void fremawork(Object msg) {
		basicLog(Formater.FRAMEWORK, msg);
	}

	public static void fremaworkf(String msg, Object ... args){
		basicLog(Formater.FRAMEWORK, msg, null, args);
	}

	//============================================== SQL ==============================================
	public static void sql(Object msg) {
		basicLog(Formater.SQL, msg);
	}

	public static void sqlf(String msg, Object ... args){
		basicLog(Formater.SQL, msg, null, args);
	}

	//============================================== DEBUG ==============================================
	public static void debug(Object msg) {
		basicLog(Formater.DEBUG, msg);
	}

	public static void debugf(String msg, Object ... args){
		basicLog(Formater.DEBUG, msg, null, args);
	}

	public static void debug(Throwable e) {
		basicLog(Formater.DEBUG, e);
	}

	public static void debug(Object msg, Throwable e) {
		basicLog(Formater.DEBUG, msg, e);
	}

	public static void debugf(String msg, Throwable e, Object ... args){
		basicLog(Formater.DEBUG, msg, e, args);
	}

	//============================================== TRACE ==============================================
	public static void trade(Object msg) {
		basicLog(Formater.TRACE, msg);
	}

	public static void tradef(String msg, Object ... args){
		basicLog(Formater.TRACE, msg, null, args);
	}

	public static void trade(Throwable e) {
		basicLog(Formater.TRACE, e);
	}

	public static void trade(Object msg, Throwable e) {
		basicLog(Formater.TRACE, msg, e);
	}

	public static void tradef(String msg, Throwable e, Object ... args){
		basicLog(Formater.TRACE, msg, e, args);
	}

	//============================================== WARN ==============================================
	public static void warn(Object msg) {
		basicLog(Formater.WARN, msg);
	}

	public static void warnf(String msg, Object ... args){
		basicLog(Formater.WARN, msg, null, args);
	}

	public static void warn(Throwable e) {
		basicLog(Formater.WARN, null, e);
	}

	public static void warn(Object msg, Throwable e) {
		basicLog(Formater.WARN, msg, e);
	}

	public static void warnf(String msg, Throwable e, Object ... args){
		basicLog(Formater.WARN, msg, e, args);
	}

	//============================================== ERROR ==============================================
	public static void error(Object msg) {
		basicLog(Formater.ERROR, msg);
	}

	public static void errorf(String msg, Object ... args){
		basicLog(Formater.ERROR, msg, null, args);
	}

	public static void error(Throwable e) {
		basicLog(Formater.ERROR, e);
	}

	public static void error(Object msg, Throwable e) {
		basicLog(Formater.ERROR, msg, e);
	}

	public static void errorf(String msg, Throwable e, Object ... args){
		basicLog(Formater.ERROR, msg, e, args);
	}

	//============================================== FATAL ==============================================
	public static void fatal(Object msg) {
		basicLog(Formater.FATAL, msg);
	}

	public static void fatalf(String msg, Object ... args){
		basicLog(Formater.FATAL, msg, null, args);
	}

	public static void fatal(Throwable e) {
		basicLog(Formater.FATAL, e);
	}

	public static void fatal(Object msg, Throwable e) {
		basicLog(Formater.FATAL, msg, e);
	}

	public static void fatalf(String msg, Throwable e, Object ... args){
		basicLog(Formater.FATAL, msg, e, args);
	}

	//============================================== FATAL ==============================================
	public static void simple(Object msg) {
		basicLog(Formater.SIMPLE, msg);
	}

	public static void simplef(String msg, Object ... args){
		basicLog(Formater.SIMPLE, msg, null, args);
	}

	//============================================== CUSTOM ==============================================
	public static void custom(String logLevel, Object msg, Throwable e) {
		basicLog(logLevel, msg, e);
	}

	public static void custom(String logLevel, Object msg) {
		basicLog(logLevel, msg, null);
	}

	public static void custom(String logLevel, Throwable e) {
		basicLog(logLevel, null, e);
	}

	public static void customf(String logLevel, String msg, Throwable e, Object ... args){
		basicLog(logLevel, TString.tokenReplace(msg, args), e);
	}

	public static void customf(String logLevel, String msg, Object ... args) {
		customf(logLevel, msg, null, args);
	}

	//============================================== BASIC_LOG ==============================================
	protected static void basicLog(Object logLevel, Object msg, Throwable e) {
		if(!Logger.isEnable()){
			return;
		}

		//自定义的日志级别
		if(logLevel instanceof String) {
			if(!hasLevel((String)logLevel)) {
				return;
			}
		}
		//系统默认的日志级别
		else if(logLevel instanceof Integer){
			if(((Integer)logLevel)>=0) {
				logLevel = formater.getLogLevel().get((Integer) logLevel);
			} else {
				return;
			}
		} else {
			System.out.println(TDateTime.now() + " [ERROR] Unknow log level [" + logLevel + "], " + msg + ", " + e);
		}

		try {
			msg = buildMessage(msg, e);
			Message message = Message.newInstance((String)logLevel, msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error: "+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	private static void basicLog(Object logLevel, Object msg) {
		basicLog(logLevel, msg, null);
	}

	private static void basicLog(Object logLevel, Throwable e) {
		basicLog(logLevel, null, e);
	}

	private static void basicLog(Object logLevel, String msg, Throwable e, Object ... args){
		if(!Logger.isEnable()){
			return;
		}

		basicLog(logLevel, TString.tokenReplace(msg, args), e);
	}

	/**
	 * 构造消息
	 * @param msg 消息对象
	 * @param exception 异常消息
	 * @return 消息
	 */
	private static String buildMessage(Object msg, Throwable exception){
		msg = TObject.nullDefault(msg, "");
		String stackMessage = "";

		if (exception == null) {
			if(msg instanceof String) {
				return msg.toString();
			} else {
				Function<Object, String> jsonFormat = LoggerStatic.JSON_FORMAT ? JSON::toJSONWithFormat : JSON::toJSON;
				return jsonFormat.apply(msg);
			}
		}

		do{
			stackMessage = stackMessage + exception.getClass().getCanonicalName() + ": " +
					exception.getMessage() + TFile.getLineSeparator() +
					TString.indent(TEnv.getStackElementsMessage(exception.getStackTrace()), 8) +
					TFile.getLineSeparator();
			exception = exception.getCause();

		} while(exception!=null);

		return (msg.toString().isEmpty() ? "" : (msg + " => ")) + stackMessage;
	}

	/**
	 * 构造消息
	 * @param msg 消息对象
	 * @return 消息
	 */
	private static String buildMessage(Object msg){
		return buildMessage(msg, null);
	}
}
