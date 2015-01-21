package org.hocate.log;

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

	public static void error(Object msg) {
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("ERROR", msg.toString());
		formater.writeFormatedLog(message);
	}

	public static void fatal(Object msg) {
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("FATAL", msg.toString());
		formater.writeFormatedLog(message);
	}
	
	public static void simple(Object msg){
		msg = msg==null?"null":msg;
		Message message = Message.newInstance("SIMPLE", msg.toString());
		formater.writeFormatedLog(message);
	}
}
