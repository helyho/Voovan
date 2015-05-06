package org.voovan.network;

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
	private static ThreadPoolExecutor threadPool = createThreadPool();

	private ThreadPool(){
	}
	
	private static ThreadPoolExecutor createThreadPool(){
		int cpuCoreCount = Runtime.getRuntime().availableProcessors();
		ThreadPoolExecutor threadPoolInstance = new ThreadPoolExecutor(cpuCoreCount*10, cpuCoreCount*10,1, TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(cpuCoreCount*10000));
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
