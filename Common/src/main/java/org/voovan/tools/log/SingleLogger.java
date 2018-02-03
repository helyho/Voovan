package org.voovan.tools.log;

import org.voovan.tools.TFile;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
	private String fileName;
	private LoggerThread loggerThread;
	private volatile static Map<String,SingleLogger> singleLoggerPool = new ConcurrentHashMap<String,SingleLogger>();
	/**
	 * 构造函数
	 * @param fileName  文件名
	 */
	public SingleLogger(String fileName){
		this.fileName = fileName;
		TFile.mkdir(fileName);
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
	public void addLogMessage(String msg) {
		loggerThread.addLogMessage(msg);
	}

	public synchronized static SingleLogger writeLog(String fileName,String msg) {
		SingleLogger singleLog = null;
		if(!singleLoggerPool.containsKey(fileName) || singleLoggerPool.get(fileName).isFinished()) {
			try {
				singleLog = new SingleLogger(fileName);
				OutputStream outputStream = null;
				outputStream = new FileOutputStream(fileName, true);
				LoggerThread loggerThread = LoggerThread.start(new OutputStream[]{outputStream});
				singleLog.setLoggerThread(loggerThread);
				synchronized (singleLoggerPool) {
					singleLoggerPool.put(fileName, singleLog);
				}
			} catch (FileNotFoundException e) {
				Logger.error("[SingleLogger] log file "+fileName+" not found.",e);
			}
		}else {
			singleLog = singleLoggerPool.get(fileName);
		}

		if(singleLog!=null) {
			singleLog.addLogMessage(msg);
		}
		return singleLog;
	}
}
