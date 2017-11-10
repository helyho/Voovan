package org.voovan.tools.threadpool;

import org.voovan.Global;
import org.voovan.tools.TProperties;

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
	protected static int MIN_POOL_SIZE = 2*cpuCoreCount;
	protected static int MAX_POOL_SIZE = 100*cpuCoreCount;

	/**
	 * 获取线程池最小活动线程数
	 * @return 线程池最小活动线程数
	 */
	public static int getMinPoolSize() {
		int minPoolTimes = TProperties.getInt(Global.getFrameworkConfigFile(), "ThreadPoolMinSize");
		MIN_POOL_SIZE = (minPoolTimes == 0 ? 2 : minPoolTimes) * cpuCoreCount;
		return MIN_POOL_SIZE;
	}

	/**
	 * 获取线程池最大活动线程数
	 * @return 线程池最大活动线程数
	 */
	public static int getMaxPoolSize() {
		int maxPoolTimes = TProperties.getInt(Global.getFrameworkConfigFile(), "ThreadPoolMaxSize");
		MIN_POOL_SIZE = (maxPoolTimes == 0 ? 100 : maxPoolTimes) * cpuCoreCount;
		return MAX_POOL_SIZE;
	}


	private ThreadPool(){
	}

	private static ThreadPoolExecutor createThreadPool(){
		ThreadPoolExecutor threadPoolInstance = new ThreadPoolExecutor(getMinPoolSize(), getMaxPoolSize(), 1, TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(cpuCoreCount*500));
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPoolInstance.allowCoreThreadTimeOut(true);

		Timer timer = new Timer("VOOVAN@THREAD_POOL_TIMER");
		ThreadPoolTask threadPoolTask = new ThreadPoolTask(threadPoolInstance);
		timer.schedule(threadPoolTask, 1, 1000);

		return threadPoolInstance;
	}

	private static ThreadPoolExecutor createThreadPool(int corePoolSize, int maxPoolSize){
		ThreadPoolExecutor threadPoolInstance = new ThreadPoolExecutor(getMinPoolSize(), getMaxPoolSize(), 1, TimeUnit.MINUTES,new ArrayBlockingQueue<Runnable>(cpuCoreCount*500));
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPoolInstance.allowCoreThreadTimeOut(true);

		Timer timer = new Timer("VOOVAN@THREAD_POOL_TIMER");
		ThreadPoolTask threadPoolTask = new ThreadPoolTask(threadPoolInstance);
		timer.schedule(threadPoolTask, 1, 1000);

		return threadPoolInstance;
	}

	public static ThreadPoolExecutor getNewThreadPool(){
		return createThreadPool();
	}
}
