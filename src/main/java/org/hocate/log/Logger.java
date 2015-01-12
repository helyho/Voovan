package org.hocate.log;

import org.hocate.tools.TEnv;

public class Logger {
	private static Formater formater = Formater.newInstance();

	public static void debug(Object msg){
		String className = TEnv.getCurrentStackInfo()[1].getClassName();
		Message message = Message.newInstance(className, "DEBUG", msg.toString());
		formater.writeLog(message);
	}
	public static void info(Object msg){
		String className = TEnv.getCurrentStackInfo()[1].getClassName();
		Message message = Message.newInstance(className, "INFO", msg.toString());
		formater.writeLog(message);
	}
	public static void warn(Object msg){
		String className = TEnv.getCurrentStackInfo()[1].getClassName();
		Message message = Message.newInstance(className, "WARN", msg.toString());
		formater.writeLog(message);
	}
	public static void error(Object msg){
		String className = TEnv.getCurrentStackInfo()[1].getClassName();
		Message message = Message.newInstance(className, "ERROR", msg.toString());
		formater.writeLog(message);
	}
	public static void fatal(Object msg){
		String className = TEnv.getCurrentStackInfo()[1].getClassName();
		Message message = Message.newInstance(className, "FATAL", msg.toString());
		formater.writeLog(message);
	}
}
