package org.voovan.tools.log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;

/**
 *	格式化日志信息并输出
 *
 *使用包括特殊的定义{{}}
 *{{t}}:制表符,正常情况下4个空格
 *{{s}}:一个空格
 *{{i}}:消息内容,即要展示的日志内容
 *{{n}}:换行符号
 *{{si}}:栈信息输出
 *{{l}}:当前代码的行号
 *{{m}}:当前代码的方法名
 *{{c}}:当前代码的类名称
 *{{t}}:当前线程名
 *{{d}}:当前代码的事件
 *{{r}}:从启动到当前代码执行的事件
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Formater {
	private String template;
	private Thread logWriterThread;
	private WriteThread writerThread;
	private List<String> logLevel;

	/**
	 * 构造函数
	 * @param template
	 * @param outputStreams
	 */
	public Formater(String template) {
		this.template = template;
		this.writerThread = new WriteThread();
		logLevel = new Vector<String>();
		for(String level : StaticParam.getLogConfig("LogLevel","ALL").split(",")){
			logLevel.add(level.trim());
		}
	}

	/**
	 * 获得当前栈元素信息
	 * @return
	 */
	public static StackTraceElement currentStackLine() {
		StackTraceElement[] stackTraceElements = TEnv.getStackElements();
		return stackTraceElements[5];
	}

	/**
	 * 获取当前线程名称
	 * @return
	 */
	private static String currentThreadName() {
		Thread currentThread = Thread.currentThread();
		return currentThread.getName()+" : "+currentThread.getId();
	}

	/**
	 * 消息缩进
	 * @param message
	 * @return
	 */
	private void preIndentMessage(Message message){
		String infoIndent = StaticParam.getLogConfig("InfoIndent","");
		if(infoIndent!=null && !infoIndent.isEmpty()){
			String msg = message.getMessage();
			if (infoIndent != null) {
				msg = infoIndent + msg;
				msg = msg.replaceAll("\n", "\n" + infoIndent);
				message.setMessage(msg);
			}
		}
	}
	
	/**
	 * 格式化消息
	 * @param message
	 * @return
	 */
	public String format(Message message) {
		
		
		Map<String, String> tokens = new HashMap<String, String>();
		StackTraceElement stackTraceElement = currentStackLine();
		//Message和栈信息公用
		tokens.put("t", "\t");
		tokens.put("s", " ");
		
		//消息缩进
		preIndentMessage(message);
		
		tokens.put("i", TString.tokenReplace(message.getMessage(), tokens));
		
		//栈信息独享
		tokens.put("n", "\r\n");
		tokens.put("p", message.getLevel()==null?"INFO":message.getLevel());
		tokens.put("si", stackTraceElement.toString());
		tokens.put("l", Integer.toString((stackTraceElement.getLineNumber())));
		tokens.put("m", stackTraceElement.getMethodName());
		tokens.put("f", stackTraceElement.getFileName());
		tokens.put("c", stackTraceElement.getClassName());
		tokens.put("t", currentThreadName());
		tokens.put("d", TDateTime.now("YYYY-MM-dd HH:mm:ss:SS z"));
		tokens.put("r", Long.toString(System.currentTimeMillis() - StaticParam.getStartTimeMillis()));
		
		return TString.tokenReplace(template, tokens);
	}

	public String simpleFormat(Message message){
		Map<String, String> tokens = new HashMap<String, String>();
		//Message和栈信息公用
		tokens.put("t", "\t");
		tokens.put("s", " ");
		
		//消息缩进
		preIndentMessage(message);
		return TString.tokenReplace(message.getMessage(), tokens);
	}
	
	/**
	 * 消息类型是否可以记录
	 * @param message
	 * @return
	 */
	public boolean messageWritable(Message message){
		if(logLevel.contains("ALL")){
			return true;
		}
		else if(logLevel.contains(message.getLevel())){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 写入消息对象,在进行格式化后的写入
	 * @param message
	 */
	public void writeFormatedLog(Message message) {
		if(messageWritable(message)){
			if(message.getLevel().equals("SIMPLE")){
				writeLog(simpleFormat(message)+"\r\n");
			}else{
				writeLog(format(message));
			}
		}
	}
	
	/**
	 * 写入消息
	 * @param msg
	 */
	public void writeLog(String msg) {
		if(Logger.isState()){
			if(logWriterThread==null || !logWriterThread.isAlive()){
				logWriterThread = new Thread(writerThread);
				logWriterThread.start();
			}
			
			writerThread.addLogMessage(msg);
		}
	}
	
	/**
	 * 获取格式化后的日志文件路径
	 * @return
	 */
	public static String getFormatedLogFilePath(){
		String logFile = StaticParam.getLogConfig("LogFile","");
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("D", TDateTime.now("YYYYMMdd"));
		tokens.put("WorkDir", TEnv.getContextPath());
		return TString.tokenReplace(logFile, tokens);
	}
	
	/**
	 * 获得一个实例
	 * @return
	 */
	public static Formater newInstance() {
			String logTemplate = StaticParam.getLogConfig("LogTemplate","{{i}}");
			return new Formater(logTemplate);
	}
	
	/**
	 * 获取输出流
	 * @return
	 */
	 protected static OutputStream[] getOutputStreams(){
		String[] LogTypes = StaticParam.getLogConfig("LogType","STDOUT").split(",");
		String logFile = getFormatedLogFilePath();
		
		try {
			OutputStream[] outputStreams = new OutputStream[LogTypes.length];
			for (int i = 0; i < LogTypes.length; i++) {
				String logType = LogTypes[i].trim();
				switch (logType) {
				case "STDOUT":
					outputStreams[i] = System.out;
					break;
				case "STDERR":
					outputStreams[i] = System.err;
					break;
				case "FILE":
					outputStreams[i] = new FileOutputStream(logFile,true);
					break;
				default:
					break;
				}
			}
			return outputStreams;
		} catch (FileNotFoundException e) {
			Logger.error(e);
			return null;
		}
	}
}
