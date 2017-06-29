package org.voovan.tools.threadpool;

import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ThreadPool {
	private final static int cpuCoreCount = Runtime.getRuntime().availableProcessors();
	public final static int MIN_POOL_SIZE = 2*cpuCoreCount;
	public final static int MAX_POOL_SIZE = 11*cpuCoreCount;

	private ThreadPool(){
	}
	
	private static ThreadPoolExecutor createThreadPool(){
		ThreadPoolExecutor threadPoolInstance = new ThreadPoolExecutor(MIN_POOL_SIZE, MAX_POOL_SIZE, 1, TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(cpuCoreCount*500));
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPoolInstance.allowCoreThreadTimeOut(true);
		Timer timer = new Timer("VOOVAN@THREAD_POOL_TIMER");
		ThreadPoolTask threadPoolTask = new ThreadPoolTask(threadPoolInstance,timer);
		timer.schedule(threadPoolTask, 1, 1000);
		return threadPoolInstance;
	}
	
	public static ThreadPoolExecutor getNewThreadPool(){
		 return createThreadPool();	
	}
}
