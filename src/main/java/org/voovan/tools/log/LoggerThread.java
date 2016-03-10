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
	private boolean finished = false;

	/**
	 * 构造函数
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
	 * @return
	 */
	public OutputStream[] getOutputStreams() {
		return outputStreams;
	}

	/**
	 * 设置日志输出流集合
	 * @param outputStreams
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
	 * @param msg
     */
	public void addLogMessage(String msg) {
		logQueue.add(msg);
	}

	@Override
	public void run() {
		while (true && !isTerminate()) {
			try {
				String formatedMessage = logQueue.poll(100, TimeUnit.MILLISECONDS);
				if (formatedMessage != null && outputStreams!=null) {
					for (OutputStream outputStream : outputStreams) {
						if (outputStream != null) {
							outputStream.write(formatedMessage.getBytes());
							outputStream.flush();
						}
					}
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		finished = true;
	}
	
	/**
	 * 检查线程是否处于结束状态
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public boolean isTerminate(){
		//应用结束的线程标识
		List<String> destoryThreadNames = TObject.newList("DestroyJavaVM","ReaderThread");
		
		//获取系统内所有的线程
		Thread[] jvmThread = TEnv.getJVMThreads();
		
		//遍历是否包含线程结束标识
		for(Thread threadObj : jvmThread){
			for(String destoryThreadName : destoryThreadNames){
				if(threadObj.getName().contains(destoryThreadName)){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 获取 Web 访问日志记录对象
	 * @return
	 */
	public static LoggerThread start(OutputStream[] outputStreams) {
		LoggerThread loggerThread = new LoggerThread(outputStreams);
		Thread loggerMainThread = new Thread(loggerThread,"VOOVAN@Logger_Thread");
		loggerMainThread.start();
		return loggerThread;
	}
	
}
