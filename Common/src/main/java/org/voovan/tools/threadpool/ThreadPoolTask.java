package org.voovan.tools.threadpool;

import org.voovan.tools.TEnv;
import org.voovan.tools.TPerformance;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池监控类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ThreadPoolTask extends HashWheelTask {
	private ThreadPoolExecutor	threadPoolInstance;
	private int					cpuCoreCount;

	public ThreadPoolTask(ThreadPoolExecutor threadPoolInstance) {
		cpuCoreCount = Runtime.getRuntime().availableProcessors();
		this.threadPoolInstance = threadPoolInstance;
	}

	@Override
	public void run() {
		if (threadPoolInstance.isShutdown()) {
			this.cancel();
		}
		
//		String threadPoolInfo = "PoolInfo:" + threadPoolInstance.getActiveCount() + "/" + threadPoolInstance.getCorePoolSize() + "/"
//				+ threadPoolInstance.getLargestPoolSize()+ "/" + threadPoolInstance.getMaximumPoolSize() + " TaskCount: "
//				+ threadPoolInstance.getCompletedTaskCount() + "/"
//				+ threadPoolInstance.getTaskCount() + " QueueSize:" + threadPoolInstance.getQueue().size() + " PerCoreLoadAvg:"
//				+ TPerformance.cpuPerCoreLoadAvg();
//		if (threadPoolInstance.getActiveCount() != 0) {
//			System.out.println(TDateTime.now() + " ShutDown:" + threadPoolInstance.isShutdown() + " " + threadPoolInfo);
//		}

		int poolSize = threadPoolInstance.getPoolSize();
		// 动态调整线程数,且系统CPU负载值要小于1
		if (threadPoolInstance.getQueue().size() > 0 ) {

			int newPoolSize = (int)(threadPoolInstance.getCorePoolSize() * 1.25);

			if(newPoolSize > ThreadPool.MAX_POOL_SIZE){
				newPoolSize = ThreadPool.MAX_POOL_SIZE;
			}

			if(newPoolSize!=poolSize) {
				threadPoolInstance.setCorePoolSize(newPoolSize);
				Logger.debug("PoolSizeChange: " + poolSize + "->" + threadPoolInstance.getCorePoolSize());
			}
		}

		else if(threadPoolInstance.getActiveCount() <= threadPoolInstance.getPoolSize()/2 &&
				 threadPoolInstance.getCorePoolSize() > ThreadPool.MIN_POOL_SIZE){

			int newPoolsize = (int)(threadPoolInstance.getCorePoolSize()*0.75);

			if(newPoolsize < ThreadPool.MIN_POOL_SIZE){
				newPoolsize = ThreadPool.MIN_POOL_SIZE;
			}

			if(newPoolsize != poolSize) {
				threadPoolInstance.setCorePoolSize(newPoolsize);
				Logger.debug("PoolSizeChange: " + poolSize + "->" + threadPoolInstance.getCorePoolSize());
			}
		}

		//如果主线程结束,则线程池也关闭
		if( TEnv.isMainThreadShutDown()) {
			threadPoolInstance.shutdown();
		}
	}
}
