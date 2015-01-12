package org.hocate.log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.hocate.tools.TDateTime;
import org.hocate.tools.TEnv;
import org.hocate.tools.TFile;
import org.hocate.tools.TProperties;
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
		return stackTraceElements[2];
	}

	private static String currentThreadName() {
		return Thread.currentThread().getName();
	}

	public String format(Message message) {
		Map<String, String> tokens = new HashMap<String, String>();
		StackTraceElement stackTraceElement = currentStackLine();
		tokens.put("p", message.getPriority());
		tokens.put("s", stackTraceElement.toString());
		tokens.put("l", Integer.toString((stackTraceElement.getLineNumber())));
		tokens.put("m", stackTraceElement.getMethodName());
		tokens.put("f", stackTraceElement.getFileName());
		tokens.put("c", stackTraceElement.getClassName());
		tokens.put("t", currentThreadName());
		tokens.put("d", TDateTime.currentTime());
		tokens.put("r", Long.toString(System.currentTimeMillis() - StaticParam.getStartTimeMillis()));
		tokens.put("n", "\r\n");
		return TString.tokenReplace(template, tokens);
	}

	public void writeLog(Message message) {
		if(logWriter==null || !logWriter.isAlive()){
			logWriter = new Thread(writeThread);
			logWriter.start();
		}
		writeThread.addLogMessage(format(message));
	}

	public static Formater newInstance() {
		OutputStream[] outputStreams;
		try {
			File configFile = TFile.getResourceFile("logger.properties");
			String logTemplate = TProperties.getString(configFile, "LogTemplate");
			String[] LogTypes = TProperties.getString(configFile, "LogType").split(",");
			String logFile = TProperties.getString(configFile, "LogFile");

			outputStreams = new OutputStream[LogTypes.length];
			for (int i = 0; i < LogTypes.length; i++) {
				String logType = LogTypes[i];
				switch (logType) {
				case "STDOUT":
					outputStreams[i] = System.out;
					break;
				case "FILE":
					outputStreams[i] = new FileOutputStream(logFile);
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
