package org.voovan.tools.log;

import org.voovan.tools.*;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 *	格式化日志信息并输出
 *
 *使用包括特殊的定义{{}}
 *{{NF}}  当前行不进行行头行尾的格式化
 *{{s}}:  一个空格
 *{{t}}:  制表符
 *{{n}}:  换行
 *================================
 *{{I}}:  消息内容,即要展示的日志内容
 *{{F}}:  源码文件名
 *{{SI}}: 栈信息输出
 *{{L}}:  当前代码的行号
 *{{M}}:  当前代码的方法名
 *{{C}}:  当前代码的类名称
 *{{T}}:  当前线程名
 *{{D}}:  当前系统时间
 *{{R}}:  从启动到当前代码执行的事件
 * ================================
 * {{F?}} 前景颜色: ?为0-7,分别为: 0=黑,1=红,2=绿,3=黄,4=蓝,5=紫,6=青,7=白
 * {{B?}} 背景颜色: ?为0-7,分别为: 0=黑,1=红,2=绿,3=黄,4=蓝,5=紫,6=青,7=白
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Formater {
	private String template;
	private volatile LoggerThread loggerThread;
	private List<String> logLevel;
	private String dateStamp;
	private int maxLineLength = -1;
	private String lineHead;
	private String lineTail;

	/**
	 * 构造函数
	 * @param template 模板
	 */
	public Formater(String template) {
		this.template = template;
		this.maxLineLength = Integer.valueOf(LoggerStatic.getLogConfig("MaxLineLength", LoggerStatic.MAX_LINE_LENGTH));
		lineHead = LoggerStatic.getLogConfig("LineHead", LoggerStatic.LINE_HEAD);
		lineTail = LoggerStatic.getLogConfig("LineTail", LoggerStatic.LINE_TAIL);

		logLevel = new Vector<String>();
		logLevel.addAll(TObject.asList(LoggerStatic.getLogConfig("LogLevel", LoggerStatic.LOG_LEVEL).split(",")));
		dateStamp = TDateTime.now("YYYYMMdd");
	}

	/**
	 * 获取日志记录级别信息
	 * @return 获取日志记录级别信息
	 */
	public List<String> getLogLevel() {
		return logLevel;
	}

	/**
	 * 获得当前栈元素信息
	 * @return 栈信息元素
	 */
	public static StackTraceElement currentStackLine() {
		StackTraceElement[] stackTraceElements = TEnv.getStackElements();
		return stackTraceElements[6];
	}

	/**
	 * 获取当前线程名称
	 * @return 当前线程名
	 */
	private static String currentThreadName() {
		Thread currentThread = Thread.currentThread();
		return currentThread.getName()+" : "+currentThread.getId();
	}

	private int realLength(String str){
		return str.replaceAll("\\{\\{n\\}\\}","").replaceAll("\\{\\{.*\\}\\}"," ").replaceAll("\033\\[\\d{2}m", "").length();
	}

	/**
	 * 消息缩进
	 * @param message 消息对象
	 * @return 随进后的消息
	 */
	private String lineFormat(Message message){
		boolean lineAlignLeft = Boolean.valueOf(LoggerStatic.getLogConfig("LineAlignLeft", LoggerStatic.LINE_ALIGN_LEFT));

		String msg = TString.tokenReplace(template, message.getTokens());

		StringBuilder msgBuilder = new StringBuilder();

		String tmpLineHead = TString.tokenReplace(lineHead, message.getTokens());
		String tmpLineTail = TString.tokenReplace(lineTail, message.getTokens());

		if(tmpLineHead!=null && tmpLineTail != null){

			String[] lines = TString.split(msg, "\n");

			for(String line : lines){
				line = line.replaceAll("[\r\n]","");
				boolean isFormatLine = line.contains("{{NF}}");

				int currentMaxLineLength = this.maxLineLength;

				//不格式化消息内容
				if(isFormatLine){
					line = line.replace("{{NF}}","");
				}else{
					currentMaxLineLength = currentMaxLineLength - realLength(tmpLineHead) - realLength(tmpLineTail);
				}

				if (isFormatLine) {
					msgBuilder.append(line).append(TFile.getLineSeparator());
					continue;
				}

				while(true) {
					int linePostion = (line.length() > currentMaxLineLength && currentMaxLineLength > 0) ? currentMaxLineLength : line.length();
					String subLine = line.substring(0, linePostion);
					line = line.substring(linePostion, line.length());

					// 不格式化,但是控制长度自动换行
//					if (isFormatLine) {
//						msgBuilder.append(line).append(TFile.getLineSeparator());
//						break;
//					}

					if (lineAlignLeft && currentMaxLineLength > 1) {
						if (subLine.endsWith(tmpLineTail)) {
							subLine = subLine.substring(0, subLine.length() - tmpLineTail.length());
						}

						int stylePatch = TString.regexMatch(subLine, "\033\\[\\d{2}m") * 5;
						subLine = TString.rightPad(subLine, currentMaxLineLength + stylePatch - 1, ' ');
					}

					msgBuilder.append(tmpLineHead)
							.append(subLine)
							.append(tmpLineTail)
							.append(TFile.getLineSeparator());

					if (line.isEmpty()) {
						break;
					}
				}
			}

		}
		return msgBuilder.toString();
	}

	/**
	 * 构造消息格式化 Token
	 * @param message 消息对象
	 */
	public void fillTokens(Message message){
		Map<String, String> tokens = new HashMap<String, String>();
		message.setTokens(tokens);
		StackTraceElement stackTraceElement = currentStackLine();

		String os = System.getProperty("os.name").toUpperCase();

		if(!os.toUpperCase().contains("WINDOWS")){
			tokens.put("F0","\033[30m");
			tokens.put("F1","\033[31m");
			tokens.put("F2","\033[32m");
			tokens.put("F3","\033[33m");
			tokens.put("F4","\033[34m");
			tokens.put("F5","\033[35m");
			tokens.put("F6","\033[36m");
			tokens.put("F7","\033[37m");
			tokens.put("FD","\033[39m");

			tokens.put("B0","\033[40m");
			tokens.put("B1","\033[41m");
			tokens.put("B2","\033[42m");
			tokens.put("B3","\033[43m");
			tokens.put("B4","\033[44m");
			tokens.put("B5","\033[45m");
			tokens.put("B6","\033[46m");
			tokens.put("B7","\033[47m");
			tokens.put("BD","\033[49m");
		} else{
			tokens.put("F0","");
			tokens.put("F1","");
			tokens.put("F2","");
			tokens.put("F3","");
			tokens.put("F4","");
			tokens.put("F5","");
			tokens.put("F6","");
			tokens.put("F7","");
			tokens.put("FD","");

			tokens.put("B0","");
			tokens.put("B1","");
			tokens.put("B2","");
			tokens.put("B3","");
			tokens.put("B4","");
			tokens.put("B5","");
			tokens.put("B6","");
			tokens.put("B7","");
			tokens.put("BD","");
		}

		//Message和栈信息公用
		tokens.put("t", "\t");
		tokens.put("s", " ");
		tokens.put("n", TFile.getLineSeparator());
		tokens.put("I", message.getMessage());


		//栈信息独享
		tokens.put("P", TObject.nullDefault(message.getLevel(),"INFO"));			//信息级别
		tokens.put("SI", stackTraceElement.toString());									//堆栈信息
		tokens.put("L", Integer.toString((stackTraceElement.getLineNumber())));			//行号
		tokens.put("M", stackTraceElement.getMethodName());								//方法名
		tokens.put("F", stackTraceElement.getFileName());								//源文件名
		tokens.put("C", stackTraceElement.getClassName());								//类名
		tokens.put("T", currentThreadName());											//线程
		tokens.put("D", TDateTime.now("YYYY-MM-dd HH:mm:ss:SS z"));						//当前时间
		tokens.put("R", Long.toString(System.currentTimeMillis() - LoggerStatic.getStartTimeMillis())); //系统运行时间
	}

	/**
	 * 格式化消息
	 * @param message 消息对象
	 * @return 格式化后的消息
	 */
	public String format(Message message) {
		fillTokens(message);
		return lineFormat(message);
	}

	/**
	 * 简单格式化
	 * @param message 消息对象
	 * @return 格式化后的消息
	 */
	public String simpleFormat(Message message){
		//消息缩进
		fillTokens(message);
		return TString.tokenReplace(message.getMessage(), message.getTokens());
	}

	/**
	 * 消息类型是否可以记录
	 * @param message 消息对象
	 * @return 是否可写入
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
	 * @param message 消息对象
	 */
	public void writeFormatedLog(Message message) {
		if(messageWritable(message)){
			if("SIMPLE".equals(message.getLevel())){
				writeLog(simpleFormat(message)+"\r\n");
			}else{
				writeLog(format(message));
			}
		}
	}

	/**
	 * 写入消息
	 * @param msg 消息字符串
	 */
	public synchronized void writeLog(String msg) {
		if(Logger.isState()){
			if (loggerThread == null || loggerThread.isFinished()) {
				this.loggerThread = LoggerThread.start(getOutputStreams());
			}
			//如果日志发生变化则产生新的文件
			if(!dateStamp.equals(TDateTime.now("YYYYMMdd"))){
				loggerThread.setOutputStreams(getOutputStreams());
			}

			//压缩历史日志文件
			packLogFile();

			loggerThread.addLogMessage(msg);
		}
	}

	/**
	 * 压缩历史日志文件
	 */
	private void packLogFile(){
		long packSize = (long) (Double.valueOf(LoggerStatic.getLogConfig("PackSize", "1024")) * 1024L * 1024L);
		String logFilePath = getFormatedLogFilePath();

		File logFile = new File(logFilePath);

		if(packSize > 0 && logFile.length() > packSize){
			try {
				File packFile = new File(logFilePath + "." + System.currentTimeMillis() + ".gz");
				TZip.encodeGZip(logFile, packFile);
			} catch (Exception e) {
				System.out.println("[ERROR] Pack log file " + logFilePath + "error: ");
				e.printStackTrace();
			}
			TFile.deleteFile(logFile);
			loggerThread.setOutputStreams(getOutputStreams());
		}
	}

	/**
	 * 获取格式化后的日志文件路径
	 * @return 返回日志文件名
	 */
	public static String getFormatedLogFilePath(){
		String filePath = "";
		String logFile = LoggerStatic.getLogConfig("LogFile", LoggerStatic.LOG_FILE);
		if(logFile!=null) {
			Map<String, String> tokens = new HashMap<String, String>();
			tokens.put("D", TDateTime.now("YYYYMMdd"));
			tokens.put("WorkDir", TFile.getContextPath());
			filePath = TString.tokenReplace(logFile, tokens);
			String fileDirectory = filePath.substring(0, filePath.lastIndexOf(File.separator));
			File loggerFile = new File(fileDirectory);
			if (!loggerFile.exists()) {
				if(!loggerFile.mkdirs()){
					System.out.println("Logger file directory error!");
				}
			}
		}else{
			filePath = null;
		}
		return filePath;
	}

	/**
	 * 获得一个实例
	 * @return 新的实例
	 */
	public static Formater newInstance() {
		String logTemplate = LoggerStatic.getLogConfig("LogTemplate", LoggerStatic.LOG_TEMPLATE);
		return new Formater(logTemplate);
	}

	/**
	 * 获取输出流
	 * @return 输出流数组
	 */
	protected static OutputStream[] getOutputStreams(){
		String[] LogTypes = LoggerStatic.getLogConfig("LogType", LoggerStatic.LOG_TYPE).split(",");
		String logFilePath = getFormatedLogFilePath();

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
					try {
						outputStreams[i] = new FileOutputStream(logFilePath,true);
					} catch (FileNotFoundException e) {
						System.out.println("log file: ["+logFilePath+"] is not found.\r\n");
					}
					break;
				default:
					break;
			}
		}
		return outputStreams;

	}
}
