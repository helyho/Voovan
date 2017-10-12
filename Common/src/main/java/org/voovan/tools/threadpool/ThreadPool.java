package org.voovan.tools.threadpool;

import org.voovan.Global;

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
	protected static int MIN_POOL_SIZE = 2*cpuCoreCount;
	protected static int MAX_POOL_SIZE = 100*cpuCoreCount;

	/**
	 * 获取线程池最小活动线程数
	 * @return 线程池最小活动线程数
	 */
	public static int getMinPoolSize() {
		return MIN_POOL_SIZE;
	}

	/**
	 * 设置线程池最小活动线程数
	 * @param minPoolSize 线程池最小活动线程数
	 */
	public static void setMinPoolSize(int minPoolSize) {
		MIN_POOL_SIZE = minPoolSize;
	}

	/**
	 * 获取线程池最大活动线程数
	 * @return 线程池最大活动线程数
	 */
	public static int getMaxPoolSize() {
		return MAX_POOL_SIZE;
	}

	/**
	 * 设置线程池最大活动线程数
	 * @param maxPoolSize 线程池最大活动线程数
	 */
	public static void setMaxPoolSize(int maxPoolSize) {
		MAX_POOL_SIZE = maxPoolSize;
	}

	private ThreadPool(){
	}

	private static ThreadPoolExecutor createThreadPool(){
		ThreadPoolExecutor threadPoolInstance = new ThreadPoolExecutor(MIN_POOL_SIZE, MAX_POOL_SIZE, 1, TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(cpuCoreCount*500));
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPoolInstance.allowCoreThreadTimeOut(true);

		Global.getHashWheelTimer().addTask(new ThreadPoolTask(threadPoolInstance), 10);
		return threadPoolInstance;
	}

	public static ThreadPoolExecutor getNewThreadPool(){
		return createThreadPool();
	}
}
