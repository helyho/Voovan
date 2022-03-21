package org.voovan.tools.log;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.Format;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * WebServer访问日志对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SingleLogger {
	private static Formater	formater	= Formater.newInstance();
	private static Map<String,SingleLogger> singleLoggerPool = new ConcurrentHashMap<String,SingleLogger>();

	private String fileName;
	private LoggerThread loggerThread;
	private OutputStream outputStream;

	/**
	 * 构造函数
	 * @param fileName  文件名
	 * @param stdout    是否在 stdout 输出日志信息
	 */
	public SingleLogger(String fileName, boolean stdout){
		this.fileName = fileName;
		TFile.mkdir(fileName);
		try {
			this.outputStream = new FileOutputStream(fileName, true);

			OutputStream[] outputStreams;
			if(stdout) {
				outputStreams = new OutputStream[]{outputStream, System.out};
			} else {
				outputStreams = new OutputStream[]{outputStream};
			}
			this.loggerThread = LoggerThread.start(outputStreams);

			synchronized (singleLoggerPool) {
				singleLoggerPool.put(fileName, this);
			}
		} catch (FileNotFoundException e) {
			Logger.error("[SingleLogger] log file "+fileName+" not found.",e);
		}
	}

	/**
	 * 构造函数
	 * @param fileName  文件名
	 */
	public SingleLogger(String fileName){
		this(fileName, false);
	}

	/**
	 * 获取日志文件名
	 * @return 日志文件名
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * 设置日志记录线程
	 * @param loggerThread 日志记录线程对象
	 */
	public void setLoggerThread(LoggerThread loggerThread) {
		this.loggerThread = loggerThread;
	}

	/**
	 * 是否完成
	 * @return 是否完成
	 */
	public boolean isFinished() {
		return loggerThread.isFinished();
	}

	/**
	 * 增加消息
	 *
	 * @param msg 消息字符串
	 */
	private void addLogMessage(String msg) {
		loggerThread.addLogMessage(msg);
	}

	public void logf(String level, Object msg, Throwable e, Object ... args){
		Message message = Message.newInstance(level == null?"SIMPLE":level, msg, args, e);
		loggerThread.addLogMessage(message);
	}

	//log msg without level
	public void logf(Object msg, Object ... args) {
		logf(null, msg, null, args);
	}

	public void log(Object msg) {
		logf(null, msg, null, null);
	}

	public void log(String msg, Throwable e) {
		logf(null, msg, e, null);
	}

	//log msg with level
	public void logf(String level, Object msg, Object ... args) {
		logf(level, msg, null, args);
	}

	public void log(String level, Object msg) {
		logf(level, msg, null, null);
	}

	public void log(String level, String msg, Throwable e) {
		logf(level, msg, e, null);
	}

	public static SingleLogger writeLog(String fileName, String msg) {
		SingleLogger singleLog = null;

		if(!singleLoggerPool.containsKey(fileName) || singleLoggerPool.get(fileName).isFinished()) {
				singleLog = new SingleLogger(fileName);
		}else {
			singleLog = singleLoggerPool.get(fileName);
		}

		if(singleLog!=null) {
			singleLog.addLogMessage(msg);
		}
		return singleLog;
	}
}
