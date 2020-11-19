package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
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
		if(formater.getLogLevel().contains("ALL")){
			return true;
		}

		if(formater.getLogLevel().contains(logLevel)){
			return true;
		}else{
			return false;
		}
	}

	public static void custom(String logLevel, Object msg, Throwable e) {
		if(!Logger.isEnable()){
			return;
		}

		if(!hasLevel(logLevel)) {
			return;
		}

		try {
			msg = buildMessage(msg, e);
			Message message = Message.newInstance(logLevel, msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error: "+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void custom(String logLevel, Object msg) {
		custom(logLevel, msg, null);
	}

	public static void custom(String logLevel, Throwable e) {
		custom(logLevel, null, e);
	}

	public static void customf(String logLevel, String msg, Throwable e, Object ... args){
		if(!Logger.isEnable()){
			return;
		}

		custom(logLevel, TString.tokenReplace(msg, args), e);
	}

	public static void customf(String logLevel, String msg, Object ... args) {
		customf(logLevel, msg, null, args);
	}

	//============================================== INFO ==============================================
	public static void info(Object msg) {
		custom("INFO", msg);
	}

	public static void infof(String msg, Object ... args){
		customf("INFO", msg, args);
	}

	//============================================== FRAMEWORK ==============================================
	public static void fremawork(Object msg) {
		custom("FRAMEWORK", msg);
	}

	public static void fremaworkf(String msg, Object ... args){
		customf("FRAMEWORK", msg, args);
	}

	//============================================== SQL ==============================================
	public static void sql(Object msg) {
		custom("SQL", msg);
	}

	public static void sqlf(String msg, Object ... args){
		customf("SQL", msg, args);
	}

	//============================================== DEBUG ==============================================
	public static void debug(Object msg) {
		custom("DEBUG", msg);
	}

	public static void debugf(String msg, Object ... args){
		customf("DEBUG", msg, args);
	}

	//============================================== WARN ==============================================
	public static void warn(Object msg) {
		custom("WARN", msg);
	}

	public static void warnf(String msg, Object ... args){
		customf("WARN", msg, args);
	}

	public static void warn(Throwable e) {
		custom("WARN", null, e);
	}

	public static void warn(Object msg, Throwable e) {
		custom("WARN", msg, e);
	}

	public static void warnf(String msg, Throwable e, Object ... args){
		customf("WARN", msg, e, args);
	}

	//============================================== ERROR ==============================================
	public static void error(Object msg) {
		custom("ERROR", msg);
	}

	public static void errorf(String msg, Object ... args){
		customf("ERROR", msg, args);
	}

	public static void error(Throwable e) {
		custom("ERROR", e);
	}

	public static void error(Object msg, Throwable e) {
		custom("ERROR", msg, e);
	}

	public static void errorf(String msg, Throwable e, Object ... args){
		customf("ERROR", msg, e, args);
	}

	//============================================== FATAL ==============================================
	public static void fatal(Object msg) {
		custom("FATAL", msg);
	}

	public static void fatalf(String msg, Object ... args){
		customf("FATAL", msg, args);
	}

	public static void fatal(Throwable e) {
		custom("FATAL", e);
	}

	public static void fatal(Object msg, Throwable e) {
		custom("FATAL", msg, e);
	}

	public static void fatalf(String msg, Throwable e, Object ... args){
		customf("FATAL", msg, e, args);
	}

	//============================================== FATAL ==============================================
	public static void simple(Object msg) {
		custom("SIMPLE", msg);
	}

	public static void simplef(String msg, Object ... args){
		customf("SIMPLE", msg, args);
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
