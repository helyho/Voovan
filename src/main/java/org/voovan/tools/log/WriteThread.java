package org.voovan.tools.log;

import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.voovan.tools.TEnv;

/**
 * 日志输出线程
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WriteThread implements Runnable {
	private ArrayBlockingQueue<String>	logQueue;
	private OutputStream[]				outputStreams;

	public WriteThread(OutputStream[] outputStreams) {
		this.logQueue = new ArrayBlockingQueue<String>(100000);
		this.outputStreams = outputStreams;
	}

	/**
	 * 增加消息
	 * 
	 * @param string
	 */
	public synchronized void addLogMessage(String string) {
		logQueue.add(string);
	}

	@Override
	public void run() {
		int loopCount = 0;
		while (true) {
			try {
				loopCount++;
				String formatedMessage = logQueue.poll(500, TimeUnit.MILLISECONDS);
				if (formatedMessage != null) {
					for (OutputStream outputStream : outputStreams) {
						if (outputStream != null) {
							outputStream.write(formatedMessage.getBytes());
							outputStream.flush();
						}
					}
				}else if(isTerminate()){
					break;
				}else if (loopCount>=10*2 && logQueue.size() == 0) {
					break;
				}
				TEnv.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 检查线程是否处于结束状态
	 * @return
	 */
	public boolean isTerminate(){
		Thread[] jvmThread = TEnv.getJVMThreads();
		for(Thread threadObj : jvmThread){
			if(threadObj.getName().contains("DestroyJavaVM") ||
					threadObj.getName().contains("ReaderThread")){
				return true;
			}
		}
		return false;
	}
	
}
