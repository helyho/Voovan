package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 日志输出线程
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class LoggerThread implements Runnable {
	private ArrayBlockingQueue<String>	logQueue;
	private OutputStream[] outputStreams;
	private boolean finished = true;

	/**
	 * 构造函数
	 * @param outputStreams 输出流数组
	 */
	public LoggerThread(OutputStream[] outputStreams) {
		this.logQueue = new ArrayBlockingQueue<String>(100000);
		this.outputStreams = outputStreams;
	}
	
	public synchronized boolean isFinished() {
		return finished;
	}
	
	/**
	 * 获取日志输出流集合
	 * @return 输出流数组
	 */
	public OutputStream[] getOutputStreams() {
		return outputStreams;
	}

	/**
	 * 设置日志输出流集合
	 * @param outputStreams 输出流数组
	 */
	public void setOutputStreams(OutputStream[] outputStreams) {
		synchronized(outputStreams){
			this.outputStreams = outputStreams;
		}
	}

	/**
	 * 关闭所有的OutputStream
	 */
	public void closeAllOutputStreams() {
		try {
			for (OutputStream outputStream : outputStreams) {
				if (outputStream != null) {
					outputStream.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 增加消息
	 *
	 * @param msg 消息字符串
     */
	public void addLogMessage(String msg) {
		logQueue.add(msg);
	}

	@Override
	public void run() {
		finished = false;
		int count = 0;
		while (true) {
			try {
				String formatedMessage = logQueue.poll(100, TimeUnit.MILLISECONDS);
				if (formatedMessage != null && outputStreams!=null) {
					for (OutputStream outputStream : outputStreams) {
						if (outputStream != null) {
							outputStream.write(formatedMessage.getBytes());
							outputStream.flush();
						}
					}
					count = 0;
				}

				if(formatedMessage==null){
					count++;
				}

				//如果有 1s 没有新的日志输出则结束当前线程
				if(count>10){
					break;
				}

			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 获取 Web 访问日志记录对象
	 * @param outputStreams 输出流数组
	 * @return 日志记录线程对象
	 */
	public static LoggerThread start(OutputStream[] outputStreams) {
		LoggerThread loggerThread = new LoggerThread(outputStreams);
		Thread loggerMainThread = new Thread(loggerThread,"VOOVAN@Logger_Thread");
		loggerMainThread.start();
		return loggerThread;
	}
	
}
