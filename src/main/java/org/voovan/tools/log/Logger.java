package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TString;

/**
 * 日志工具类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Logger {
	private static Formater	formater	= Formater.newInstance();

	public static void debug(Object msg) {
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("DEBUG", msg.toString());
		formater.writeFormatedLog(message);
	}

	public static void info(Object msg) {
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("INFO", msg.toString());
		formater.writeFormatedLog(message);
	}

	public static void warn(Object msg) {
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("WARN", msg.toString());
		formater.writeFormatedLog(message);
	}
	
	public static void warn(Exception e) {
		String msg = e.getClass().getCanonicalName() + ": " + e.getMessage() + "\r\n"
				+ TString.indent(TEnv.getStackElementsMessage(e.getStackTrace()), 8);
		Message message = Message.newInstance("WARN", msg);
		formater.writeFormatedLog(message);
	}

	public static void warn(Object msg,Exception e) {
		msg = msg==null?"null":msg;
		String msgStr = e.getClass().getCanonicalName() + ": " + msg + "\r\n"
				+ TString.indent(TEnv.getStackElementsMessage(e.getStackTrace()), 8);
		Message message = Message.newInstance("WARN", msgStr);
		formater.writeFormatedLog(message);
	}

	public static void error(Object msg) {
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("ERROR", msg.toString());
		formater.writeFormatedLog(message);
	}
	
	public static void error(Exception e) {
		String msg = e.getClass().getCanonicalName() + ": " + e.getMessage() + "\r\n"
				+ TString.indent(TEnv.getStackElementsMessage(e.getStackTrace()), 8);
		Message message = Message.newInstance("ERROR", msg);
		formater.writeFormatedLog(message);
	}
	
	public static void error(Object msg,Exception e) {
		msg = msg==null?"null":msg;
		String msgStr = e.getClass().getCanonicalName() + ": " + msg + "\r\n"
				+ TString.indent(TEnv.getStackElementsMessage(e.getStackTrace()), 8);
		Message message = Message.newInstance("ERROR", msgStr);
		formater.writeFormatedLog(message);
	}

	public static void fatal(Object msg) {
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("FATAL", msg.toString());
		formater.writeFormatedLog(message);
	}
	
	public static void fatal(Exception e) {
		String msg = e.getClass().getCanonicalName() + ": " + e.getMessage() + "\r\n"
				+ TString.indent(TEnv.getStackElementsMessage(e.getStackTrace()), 8);
		Message message = Message.newInstance("FATAL", msg);
		formater.writeFormatedLog(message);
	}
	
	public static void fatal(Object msg,Exception e) {
		msg = msg==null?"null":msg;
		String msgStr = e.getClass().getCanonicalName() + ": " + msg + "\r\n"
				+ TString.indent(TEnv.getStackElementsMessage(e.getStackTrace()), 8);
		Message message = Message.newInstance("FATAL", msgStr);
		formater.writeFormatedLog(message);
	}
	
	public static void simple(Object msg){
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("SIMPLE", msg.toString());
		formater.writeFormatedLog(message);
	}
}
