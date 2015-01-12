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

	public Formater(String template, OutputStream[] outputStreams) {
		this.template = template;
		this.writeThread = new WriteThread(outputStreams);
	}

	public static StackTraceElement currentStackLine() {
		StackTraceElement[] stackTraceElements = TEnv.getCurrentStackInfo();
		return stackTraceElements[5];
	}

	private static String currentThreadName() {
		return Thread.currentThread().getName();
	}

	public String preIndentMessage(Message message){
		String infoIndent = StaticParam.getConfig("InfoIndent");
		String msg = message.getMessage();
		if (infoIndent != null) {
			msg = infoIndent + msg;
			msg = msg.replaceAll("\r\n", "\r\n" + infoIndent);
			return msg.replaceAll("\n", "\n" + infoIndent);
		}else{
			return msg;
		}
	}
	
	public String format(Message message) {
		Map<String, String> tokens = new HashMap<String, String>();
		StackTraceElement stackTraceElement = currentStackLine();
		//Message 和栈信息公用
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

	public void writeFormatedLog(Message message) {
		writeLog(format(message));
	}
	
	public void writeLog(String msg) {
		if(logWriter==null || !logWriter.isAlive()){
			logWriter = new Thread(writeThread);
			logWriter.start();
		}
		writeThread.addLogMessage(msg);
	}
	
	public void writeSimpleLog(Message message) {
		if(logWriter==null || !logWriter.isAlive()){
			logWriter = new Thread(writeThread);
			logWriter.start();
		}
		writeThread.addLogMessage(message.getMessage());
	}

	public static Formater newInstance() {
		OutputStream[] outputStreams;
		try {
			
			String logTemplate = StaticParam.getConfig("LogTemplate");
			String[] LogTypes = StaticParam.getConfig("LogType").split(",");
			String logFile = StaticParam.getConfig("LogFile");

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
