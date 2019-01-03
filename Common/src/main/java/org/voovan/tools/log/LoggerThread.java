package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TString;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeoutException;
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
	private ConcurrentLinkedDeque<String> logQueue;
	private OutputStream[] outputStreams;
	private volatile AtomicBoolean finished = new AtomicBoolean(false);
	private volatile int pause = 0; // 0: 正常 , 1: 暂停中, 2: 暂停

	/**
	 * 构造函数
	 * @param outputStreams 输出流数组
	 */
	public LoggerThread(OutputStream[] outputStreams) {
		this.logQueue = new ConcurrentLinkedDeque<String>();
		this.outputStreams = outputStreams;

	}

	/**
	 * 是否已经结束
	 * @return true: 结束, false: 运行中
	 */
	public boolean isFinished() {
		return finished.get();
	}

	/**
	 * 暂停日志输出
	 * @return true: 成功, false: 失败
	 */
	public boolean pause(){
		pause = 1;
		try {
			TEnv.wait(3000, ()-> pause != 2);
			return true;
		} catch (TimeoutException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 恢复日志输出
	 */
	public void unpause(){
		pause = 0;
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
	 * 刷新日志到目标设备
	 */
	public void flush(){
		for (OutputStream outputStream : outputStreams) {
			if (outputStream != null) {
				try {
					outputStream.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 增加消息
	 *
	 * @param msg 消息字符串
	 */
	public void addLogMessage(String msg) {
		logQueue.offer(msg);
	}

	@Override
	public void run() {
		String formatedMessage = null;

		Thread mainThread = TEnv.getMainThread();

		boolean needFlush = false;

		try {
			while (true) {

				if(this.pause == 1){
					flush();
					this.pause = 2;
				}

				if(this.pause != 0){
					Thread.sleep(1);
					continue;
				}

				formatedMessage = logQueue.poll();

				//优化日志输出事件
				if(formatedMessage == null) {

					if(needFlush) {
						flush();
						needFlush = false;
					}

					Thread.sleep(1);
					continue;
				}

				if (formatedMessage != null && outputStreams!=null) {
					for (OutputStream outputStream : outputStreams) {
						if (outputStream != null) {
							if(LoggerStatic.HAS_COLOR && !(outputStream instanceof PrintStream)){
								//文件写入剔除出着色部分
								formatedMessage = TString.fastReplaceAll(formatedMessage, "\033\\[\\d{2}m", "");
							}
							outputStream.write(formatedMessage.getBytes());

							needFlush = true;
						}
					}
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
		loggerMainThread.setDaemon(true);
		loggerMainThread.start();
		return loggerThread;
	}

}
