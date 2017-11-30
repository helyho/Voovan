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
	protected static int MIN_POOL_SIZE = 10*cpuCoreCount;
	protected static int MAX_POOL_SIZE = 100*cpuCoreCount;

	/**
	 * 获取线程池最小活动线程数
	 * @return 线程池最小活动线程数
	 */
	public static int getMinPoolSize() {
		int minPoolTimes = TProperties.getInt(Global.getFrameworkConfigFile(), "ThreadPoolMinSize");
		MIN_POOL_SIZE = (minPoolTimes == 0 ? 2 : minPoolTimes) * cpuCoreCount;
		MIN_POOL_SIZE = MIN_POOL_SIZE < 10 ? 10 : MIN_POOL_SIZE;
		return MIN_POOL_SIZE;
	}

	/**
	 * 获取线程池最大活动线程数
	 * @return 线程池最大活动线程数
	 */
	public static int getMaxPoolSize() {
		int maxPoolTimes = TProperties.getInt(Global.getFrameworkConfigFile(), "ThreadPoolMaxSize");
		MAX_POOL_SIZE = (maxPoolTimes == 0 ? 100 : maxPoolTimes) * cpuCoreCount;
		return MAX_POOL_SIZE;
	}


	private ThreadPool(){
	}

	private static ThreadPoolExecutor createThreadPool(){
		ThreadPoolExecutor threadPoolInstance = createThreadPool(getMinPoolSize(), getMaxPoolSize(), 1000*60);

		//启动线程池自动调整任务
		Timer timer = new Timer("VOOVAN@THREAD_POOL_TIMER");
		ThreadPoolTask threadPoolTask = new ThreadPoolTask(threadPoolInstance);
		timer.schedule(threadPoolTask, 1, 1000);
		return threadPoolInstance;
	}

	/**
	 * 创建线程池
	 * @param mimPoolSize 最小线程数
	 * @param maxPoolSize 最大线程数
	 * @param threadTimeout 线程闲置超时时间
	 * @return 线程池对象
	 */
	public static ThreadPoolExecutor createThreadPool(int mimPoolSize, int maxPoolSize, int threadTimeout){
		ThreadPoolExecutor threadPoolInstance = new ThreadPoolExecutor(mimPoolSize, maxPoolSize, threadTimeout, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(cpuCoreCount*500));
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPoolInstance.allowCoreThreadTimeOut(true);

		return threadPoolInstance;
	}

	public static ThreadPoolExecutor getNewThreadPool(){
		return createThreadPool();
	}
}
