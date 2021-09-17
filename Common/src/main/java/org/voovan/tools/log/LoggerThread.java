package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TString;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Formatter;
import java.util.concurrent.ConcurrentLinkedDeque;
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
	private ConcurrentLinkedDeque<Object> logQueue;
	private ConcurrentLinkedDeque<String> logCacheQueue;
	private OutputStream[] outputStreams;
	private AtomicBoolean finished = new AtomicBoolean(false);
	private Thread thread;
	private int pause = 0; // 0: 正常 , 1: 暂停中, 2: 暂停

	/**
	 * 构造函数
	 * @param outputStreams 输出流数组
	 */
	public LoggerThread(OutputStream[] outputStreams) {
		this.logQueue = new ConcurrentLinkedDeque<Object>();
		this.logCacheQueue = new ConcurrentLinkedDeque<String>();
		this.outputStreams = outputStreams;

		TEnv.addShutDownHook(()->{
			while(!logQueue.isEmpty() || !logCacheQueue.isEmpty()) {
				TEnv.sleep(10);
				//如果日志输出关闭,则自动输出
				if(!Logger.isEnable() || !thread.isAlive()) {
					try {
						output();
						flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			Logger.setEnable(false);
		});
	}

	/**
	 * 是否已经结束
	 * @return true: 结束, false: 运行中
	 */
	public boolean isFinished() {
		return finished.get() || !thread.isAlive();
	}

	/**
	 * 暂停日志输出
	 * @return true: 成功, false: 失败
	 */
	public boolean pause(){
		pause = 1;
		return TEnv.wait(3000, ()-> pause != 2);
	}

	/**
	 * 恢复日志输出
	 */
	public void unpause(){
		pause = 0;
	}

	public Thread getThread() {
		return thread;
	}

	protected void setThread(Thread thread) {
		this.thread = thread;
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
		if(outputStreams != null) {
			for (OutputStream outputStream : getOutputStreams()) {
				if (outputStream instanceof FileOutputStream) {
					try {
						((FileOutputStream) outputStream).close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		this.outputStreams = outputStreams;
	}


	/**
	 * 增加日志消息
	 *
	 * @param msg 消息字符串
	 */
	public void addLogMessage(Object msg) {
		logQueue.offer(msg);
	}

	@Override
	public void run() {
		try {
			while (Logger.isEnable()) {

				try {

					if (this.pause == 1) {
						flush();
						this.pause = 2;
					}

					if (this.pause != 0) {
						Thread.sleep(100);
						continue;
					}

					//优化日志输出事件
					if (logQueue.isEmpty()) {
						flush();
						TEnv.sleep(10);
						continue;
					}

					output();


				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			System.out.println("[FRAMEWORK] Main logger thread is terminaled");

			flush();
		} finally {
			close();
			finished.set(true);
		}
	}

	public String renderMessage(Object obj){
		String formatedMessage = null;
		//消息转换
		if (obj instanceof String) {
			formatedMessage = (String) obj;
		} else if(obj instanceof Message) {
			Message msg = (Message) obj;

			msg.format();

			if("SIMPLE".equals(msg.getLevel())){
				formatedMessage = Logger.formater.simpleFormat(msg)+"\r\n";
			}else{
				formatedMessage = Logger.formater.format(msg);
			}
		}

		return formatedMessage;
	}

	/**
	 * 输出日志
	 * @return true: 有日志输出, false: 无日志输出
	 * @throws IOException IO 异常
	 */
	public boolean output() throws IOException {
		Object obj = logQueue.poll();
		String formatedMessage = renderMessage(obj);

		if (formatedMessage != null && this.outputStreams != null) {
			for (OutputStream outputStream : outputStreams) {
				if (outputStream != null) {
					if (LoggerStatic.HAS_COLOR && !(outputStream instanceof PrintStream)) {
						//文件写入剔除出着色部分
						formatedMessage = TString.fastReplaceAll(formatedMessage, "\033\\[\\d{2}m", "");
					}

					//对于文件输出检测
					if(outputStream instanceof FileOutputStream) {
						//文件如果关闭则将消息加入缓冲,跳过
						if(!((FileOutputStream)outputStream).getChannel().isOpen()){
							logCacheQueue.add(formatedMessage);
							continue;
						} else {
							//如果文件输出可用,则尝试输出上一步缓冲中的消息
							while(true) {
								String message = logCacheQueue.poll();
								if(message!=null) {
									outputStream.write(message.getBytes());
								} else {
									break;
								}
							}
						}
					}

					outputStream.write(formatedMessage.getBytes());
				}
			}

			return true;
		} else {
			return false;
		}
	}

	/**
	 * 关闭所有的OutputStream
	 */
	public void close() {
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
	 * 获取 Web 访问日志记录对象
	 * @param outputStreams 输出流数组
	 * @return 日志记录线程对象
	 */
	public synchronized static LoggerThread start(OutputStream[] outputStreams) {
		LoggerThread loggerThread = new LoggerThread(outputStreams);
		Thread loggerMainThread = new Thread(loggerThread,"VOOVAN@LOGGER_THREAD");
		loggerMainThread.setDaemon(true);
		loggerMainThread.setPriority(1);
		loggerMainThread.start();

		loggerThread.setThread(loggerMainThread);
		return loggerThread;
	}

}
