package org.hocate.log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.hocate.tools.TDateTime;
import org.hocate.tools.TEnv;
import org.hocate.tools.TString;

public class Formater {
	private String template;
	private Thread logWriter;
	private WriteThread writeThread;

	/**
	 * 构造函数
	 * @param template
	 * @param outputStreams
	 */
	public Formater(String template, OutputStream[] outputStreams) {
		this.template = template;
		this.writeThread = new WriteThread(outputStreams);
	}

	/**
	 * 获得当前栈元素信息
	 * @return
	 */
	public static StackTraceElement currentStackLine() {
		StackTraceElement[] stackTraceElements = TEnv.getCurrentStackInfo();
		return stackTraceElements[5];
	}

	/**
	 * 获取当前线程名称
	 * @return
	 */
	private static String currentThreadName() {
		return Thread.currentThread().getName();
	}

	/**
	 * 消息缩进
	 * @param message
	 * @return
	 */
	public String preIndentMessage(Message message){
		String infoIndent = StaticParam.getLogConfig("InfoIndent");
		String msg = message.getMessage();
		if (infoIndent != null) {
			msg = infoIndent + msg;
			msg = msg.replaceAll("\r\n", "\r\n" + infoIndent);
			return msg.replaceAll("\n", "\n" + infoIndent);
		}else{
			return msg;
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
		preIndentMessage(message);
		tokens.put("i", TString.tokenReplace(message.getMessage(), tokens));
		
		//栈信息独享
		tokens.put("n", "\r\n");
		tokens.put("p", message.getPriority());
		tokens.put("si", stackTraceElement.toString());
		tokens.put("l", Integer.toString((stackTraceElement.getLineNumber())));
		tokens.put("m", stackTraceElement.getMethodName());
		tokens.put("f", stackTraceElement.getFileName());
		tokens.put("c", stackTraceElement.getClassName());
		tokens.put("t", currentThreadName());
		tokens.put("d", TDateTime.currentTime("YYYYMMDDHHmmss"));
		tokens.put("r", Long.toString(System.currentTimeMillis() - StaticParam.getStartTimeMillis()));
		
		return TString.tokenReplace(template, tokens);
	}

	/**
	 * 写入格式化后的消息
	 * @param message
	 */
	public void writeFormatedLog(Message message) {
		writeLog(format(message));
	}
	
	/**
	 * 写入消息
	 * @param msg
	 */
	public void writeLog(String msg) {
		if(logWriter==null || !logWriter.isAlive()){
			logWriter = new Thread(writeThread);
			logWriter.start();
		}
		writeThread.addLogMessage(msg);
	}

	/**
	 * 获得一个实例
	 * @return
	 */
	public static Formater newInstance() {
		OutputStream[] outputStreams;
		try {
			
			String logTemplate = StaticParam.getLogConfig("LogTemplate");
			String[] LogTypes = StaticParam.getLogConfig("LogType").split(",");
			String logFile = StaticParam.getLogConfig("LogFile");

			outputStreams = new OutputStream[LogTypes.length];
			for (int i = 0; i < LogTypes.length; i++) {
				String logType = LogTypes[i];
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
			return new Formater(logTemplate, outputStreams);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
}
