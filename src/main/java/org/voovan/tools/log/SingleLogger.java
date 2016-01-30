package org.voovan.tools.log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

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
	
	/**
	 * 构造函数
	 * @throws FileNotFoundException 
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
	 * @param string
	 */
	public void addLogMessage(String msg) {
		loggerThread.addLogMessage(msg);
	}
	
	public static SingleLogger start(String fileName) throws FileNotFoundException {
		SingleLogger singleLog = new SingleLogger(fileName);
		OutputStream outputStream = new FileOutputStream(fileName,true);
		LoggerThread loggerThread = LoggerThread.start(new OutputStream[]{outputStream});
		singleLog.setLoggerThread(loggerThread);
		return singleLog;
	}
}
