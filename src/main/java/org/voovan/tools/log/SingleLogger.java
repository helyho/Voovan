package org.voovan.tools.log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;

/**
 * HttpServer访问日志对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SingleLogger {
	private String fileName;
	private LoggerThread loggerThread;
	private static HashMap<String,SingleLogger> singleLoggerPool = new HashMap<String,SingleLogger>();
	/**
	 * 构造函数
	 */
	public SingleLogger(String fileName){
		this.fileName = fileName;
		
	}

	/**
	 * 获取日志文件名
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}


	public void setLoggerThread(LoggerThread loggerThread) {
		this.loggerThread = loggerThread;
	}

	public boolean isFinished() {
		return loggerThread.isFinished();
	}
	
	/**
	 * 增加消息
	 * 
	 * @param msg
	 */
	public void addLogMessage(String msg) {
		loggerThread.addLogMessage(msg);
	}
	
	public static synchronized SingleLogger writeLog(String fileName,String msg) {
		SingleLogger singleLog = null;
		if(!singleLoggerPool.containsKey(fileName) || singleLoggerPool.get(fileName).isFinished()) {
			try {
				singleLog = new SingleLogger(fileName);
				OutputStream outputStream = null;
				outputStream = new FileOutputStream(fileName, true);
				LoggerThread loggerThread = LoggerThread.start(new OutputStream[]{outputStream});
				singleLog.setLoggerThread(loggerThread);
				singleLoggerPool.put(fileName,singleLog);
			} catch (FileNotFoundException e) {
				Logger.error("[SingleLogger] log file "+fileName+" not found.",e);
			}
		}else {
			singleLog = singleLoggerPool.get(fileName);
		}
		singleLog.addLogMessage(msg);
		return singleLog;
	}
}
