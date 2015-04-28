package org.hocate.network;

import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;




/**
 * 线程池
 * @author helyho
 *
 */
public class ThreadPool {
	private static ThreadPoolExecutor threadPool = createThreadPool();

	private ThreadPool(){
	}
	
	private static ThreadPoolExecutor createThreadPool(){
		int cpuCoreCount = Runtime.getRuntime().availableProcessors();
		ThreadPoolExecutor threadPoolInstance = new ThreadPoolExecutor(cpuCoreCount*10, cpuCoreCount*10,1, TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPoolInstance.allowCoreThreadTimeOut(true);
		Timer timer = new Timer();
		ThreadPoolTask threadPoolTask = new ThreadPoolTask(threadPoolInstance,timer);
		timer.schedule(threadPoolTask, 1, 1000);
		return threadPoolInstance;
	}
	
	public static ThreadPoolExecutor getThreadPool(){
		return threadPool;
	}
}
