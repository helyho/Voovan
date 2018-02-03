package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TString;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
	private volatile AtomicBoolean finished = new AtomicBoolean(false);

	/**
	 * 构造函数
	 * @param outputStreams 输出流数组
	 */
	public LoggerThread(OutputStream[] outputStreams) {
		this.logQueue = new ArrayBlockingQueue<String>(100000);
		this.outputStreams = outputStreams;

	}

	public boolean isFinished() {
		return finished.get();
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
		this.outputStreams = outputStreams;
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
		String formatedMessage = null;

		Thread mainThread = TEnv.getMainThread();
		try {
			while (true) {

				//优化日志输出事件
				if(logQueue.size() == 0){
					TEnv.sleep(1);
					continue;
				}

				formatedMessage = logQueue.poll(1, TimeUnit.MILLISECONDS);
				if (formatedMessage != null && outputStreams!=null) {
					for (OutputStream outputStream : outputStreams) {
						if (outputStream != null) {
							if(!(outputStream instanceof PrintStream)){
								//文件写入踢出着色部分
								formatedMessage = TString.fastReplaceAll(formatedMessage, "\033\\[\\d{2}m", "");
							}
							outputStream.write(formatedMessage.getBytes());
							outputStream.flush();
						}
					}
				}
				//如果主线程结束,则日志线程也退出
				if(mainThread !=null && mainThread.getState() == Thread.State.TERMINATED){
					break;
				}

				if(mainThread == null){
					break;
				}
			}

			finished.set(true);

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
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

	}

	/**
	 * 获取 Web 访问日志记录对象
	 * @param outputStreams 输出流数组
	 * @return 日志记录线程对象
	 */
	public synchronized static LoggerThread start(OutputStream[] outputStreams) {
		LoggerThread loggerThread = new LoggerThread(outputStreams);
		Thread loggerMainThread = new Thread(loggerThread,"VOOVAN@LOGGER_THREAD");
		loggerMainThread.start();
		return loggerThread;
	}

}
