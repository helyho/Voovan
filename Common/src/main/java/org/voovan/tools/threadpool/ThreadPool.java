package org.voovan.tools.threadpool;

import org.voovan.tools.TEnv;
import org.voovan.tools.TProperties;

import java.util.Timer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
	private static int cpuCoreCount = Runtime.getRuntime().availableProcessors();

	protected static int MIN_POOL_SIZE = cpuCoreCount;
	protected static int MAX_POOL_SIZE = cpuCoreCount;
	protected static int STATUS_INTERVAL = 5000;

	protected static int minPoolSize = TProperties.getInt("framework", "ThreadPoolMinSize");
	protected static int maxPoolSize = TProperties.getInt("framework", "ThreadPoolMaxSize");

	protected static ConcurrentHashMap<String, ThreadPoolExecutor> THREAD_POOL_HANDLER = new ConcurrentHashMap<String, ThreadPoolExecutor>();

	/**
	 * 获取线程池最小活动线程数
	 * @return 线程池最小活动线程数
	 */
	public static int getMinPoolSize() {
		MIN_POOL_SIZE = minPoolSize == -1 ? cpuCoreCount : minPoolSize;
		return MIN_POOL_SIZE;
	}

	/**
	 * 获取线程池最大活动线程数
	 * @return 线程池最大活动线程数
	 */
	public static int getMaxPoolSize() {
		if(maxPoolSize > maxPoolSize){
			maxPoolSize = maxPoolSize;
		}
		MAX_POOL_SIZE = maxPoolSize == -1 ? cpuCoreCount : maxPoolSize;
		return MAX_POOL_SIZE;
	}

	/**
	 * 获取线程池最大活动线程数
	 * @return 线程池最大活动线程数
	 */
	public static int getStatusInterval() {
		STATUS_INTERVAL = TProperties.getInt("framework", "ThreadPoolStatusInterval", 0);
		return STATUS_INTERVAL;
	}

	static{
		getMinPoolSize();
		getMaxPoolSize();
		getStatusInterval();
	}

	private ThreadPool(){
	}

    /**
     * 使用默认配置构造线程池, 配置来自于 fraemwork.properties
     * @param poolName 线程池名称
     * @return 线程池对象
     */
	public static ThreadPoolExecutor createThreadPool(String poolName){
		System.out.println("[THREAD_POOL] " + poolName + " Min size: " + MIN_POOL_SIZE);
		System.out.println("[THREAD_POOL] " + poolName + " Max size: " + MAX_POOL_SIZE);

		ThreadPoolExecutor threadPoolInstance = createThreadPool(poolName, MIN_POOL_SIZE, MAX_POOL_SIZE, 1000*60);

		//启动线程池自动调整任务
		if(STATUS_INTERVAL>0) {
			Timer timer = new Timer("VOOVAN@THREAD_POOL_TIMER", true);
			ThreadPoolTask threadPoolTask = new ThreadPoolTask(threadPoolInstance);
			timer.schedule(threadPoolTask, 1, 1000);
		}
		return threadPoolInstance;
	}


	/**
	 * 创建线程池
	 * @param poolName 池的名称
	 * @param mimPoolSize 最小线程数
	 * @param maxPoolSize 最大线程数
	 * @param keepAliveTime 线程闲置最大存活时间,单位: 毫秒
	 * @return 线程池对象
	 */
	public static ThreadPoolExecutor createThreadPool(String poolName, int mimPoolSize, int maxPoolSize, int keepAliveTime){

		return createThreadPool(poolName, mimPoolSize, maxPoolSize, keepAliveTime, true , 5);
	}

		/**
		 * 创建线程池
		 * @param poolName 池的名称
		 * @param mimPoolSize 最小线程数
		 * @param maxPoolSize 最大线程数
		 * @param keepAliveTime 线程闲置最大存活时间, 单位: 毫秒
		 * @param daemon 是否是守护线程
		 * @param priority 线程优先级
		 * @return 线程池对象
		 */
		public static ThreadPoolExecutor createThreadPool(String poolName, int mimPoolSize, int maxPoolSize, int keepAliveTime, boolean daemon, int priority) {
			return createThreadPool(poolName, mimPoolSize, maxPoolSize, keepAliveTime, daemon, priority, cpuCoreCount * 100);
		}

		/**
         * 创建线程池
         * @param poolName 池的名称
         * @param mimPoolSize 最小线程数
         * @param maxPoolSize 最大线程数
         * @param keepAliveTime 线程闲置最大存活时间, 单位: 毫秒
         * @param daemon 是否是守护线程
         * @param priority 线程优先级
		 * @param queueSize 线程池任务队列大小
         * @return 线程池对象
         */
	public static ThreadPoolExecutor createThreadPool(String poolName, int mimPoolSize, int maxPoolSize, int keepAliveTime, boolean daemon, int priority, int queueSize){
		ThreadPoolExecutor threadPoolInstance = THREAD_POOL_HANDLER.get(poolName);

		if(threadPoolInstance==null) {
			threadPoolInstance = new ThreadPoolExecutor(mimPoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(queueSize), new DefaultThreadFactory(poolName, daemon, priority));
			//设置allowCoreThreadTimeOut,允许回收超时的线程
			threadPoolInstance.allowCoreThreadTimeOut(true);

			THREAD_POOL_HANDLER.put(poolName, threadPoolInstance);
			ThreadPoolExecutor finalThreadPoolInstance = threadPoolInstance;
			TEnv.addShutDownHook(()->{
				finalThreadPoolInstance.shutdown();
				return true;
			});
		}

		return threadPoolInstance;
	}

	/**
	 * 平滑的关闭线程池
	 * @param threadPoolExecutor 线程池对象
	 */
	public static void gracefulShutdown(ThreadPoolExecutor threadPoolExecutor) {
		threadPoolExecutor.shutdown();

		TEnv.wait(()-> !threadPoolExecutor.isShutdown());
	}
}
