package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;

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
	private static boolean state = true;

	/**
	 * 日志输出状态
	 *
	 * @return true:输出日志,false 不输出任何日志
	 */
	public static boolean isState() {
		return state;
	}

	/**
	 * 设置日志输出状态
	 * @param state true:输出日志,false 不输出任何日志
	 */
	public static void setState(boolean state) {
		Logger.state = state;
	}

	/**
	 * 判断是否包含指定的日志级别
	 * @param logLevel  日志级别
	 * @return true: 包含, false: 不包含
	 */
	public static boolean isLogLevel(String logLevel){
		if(formater.getLogLevel().contains("ALL")){
			return true;
		}

		if(formater.getLogLevel().contains(logLevel)){
			return true;
		}else{
			return false;
		}
	}

	public static void info(Object msg) {
		try {
			msg = buildMessage(msg);
			Message message = Message.newInstance("INFO", msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void infof(String msg, Object ... args){
		info(TString.tokenReplace(msg, args));
	}

	public static void debug(Object msg) {
		try {
			msg = buildMessage(msg);
			Message message = Message.newInstance("DEBUG", msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void debugf(String msg, Object ... args){
		debug(TString.tokenReplace(msg, args));
	}


	public static void warn(Object msg) {
		try {
			msg = buildMessage(msg);
			Message message = Message.newInstance("WARN", msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void warnf(String msg, Object ... args){
		warn(TString.tokenReplace(msg, args));
	}


	public static void warn(Exception e) {
		try {
			String msg = buildMessage(null, e);
			Message message = Message.newInstance("WARN", msg);
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + e.getMessage());
		}
	}

	public static void warn(Object msg, Exception e) {
		try {
			String msgStr = buildMessage(msg, e);
			Message message = Message.newInstance("WARN", msgStr);
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void warnf(String msg, Exception e, Object ... args){
		warn(TString.tokenReplace(msg, args), e);
	}


	public static void error(Object msg) {
		try {
			msg = buildMessage(msg, null);
			Message message = Message.newInstance("ERROR", msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void errorf(String msg, Object ... args){
		error(TString.tokenReplace(msg, args));
	}

	public static void error(Exception e) {
		try {
			String msg = buildMessage(null, e);
			Message message = Message.newInstance("ERROR", msg);
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + e.getMessage());
		}
	}

	public static void error(Object msg, Exception e) {
		try {
			String msgStr = buildMessage(msg, e);
			Message message = Message.newInstance("ERROR", msgStr);
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void errorf(String msg, Exception e, Object ... args){
		error(TString.tokenReplace(msg, args), e);
	}

	public static void fatal(Object msg) {
		try {
			msg = buildMessage(msg);
			Message message = Message.newInstance("FATAL", msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void fatalf(String msg, Object ... args){
		fatal(TString.tokenReplace(msg, args));
	}

	public static void fatal(Exception e) {
		try {
			String msg = buildMessage(e.getMessage(), e);
			Message message = Message.newInstance("FATAL", msg);
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + e.getMessage());
		}
	}

	public static void fatal(Object msg, Exception e) {
		try {
			String msgStr = buildMessage(msg, e);
			Message message = Message.newInstance("FATAL", msgStr);
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			simple("Logger system error:"+oe.getMessage()+"\r\n");
			simple(TEnv.getStackElementsMessage(oe.getStackTrace()));
			simple("Output message is: " + msg);
		}
	}

	public static void fatalf(String msg, Exception e, Object ... args){
		fatal(TString.tokenReplace(msg, args), e);
	}


	public static void simple(Object msg) {
		try {
			msg = buildMessage(msg);
			Message message = Message.newInstance("SIMPLE", msg.toString());
			formater.writeFormatedLog(message);
		} catch (Exception oe) {
			System.out.println("Logger system error:"+oe.getMessage()+"\r\n");
			System.out.println(TEnv.getStackElementsMessage(oe.getStackTrace()));
			System.out.println("Output message is: " + msg);
		}
	}

	public static void simplef(String msg, Object ... args){
		error(TString.tokenReplace(msg, args));
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
			return msg.toString();
		}

		do{
			stackMessage = stackMessage + exception.getClass().getCanonicalName() + ": " +
					exception.getMessage() + TFile.getLineSeparator() +
					TString.indent(TEnv.getStackElementsMessage(exception.getStackTrace()),8) +
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
