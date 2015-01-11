package org.hocate.network;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 * @author helyho
 *
 */
public class ThreadPool {

	public static ThreadPoolExecutor getThreadPool(){
		int cpuCoreCount = Runtime.getRuntime().availableProcessors();
		ThreadPoolExecutor eventThreadPool = new ThreadPoolExecutor(cpuCoreCount*100, cpuCoreCount*200,1, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		eventThreadPool.allowCoreThreadTimeOut(true);
		return eventThreadPool;
	}
	
}
