package org.voovan.tools.threadpool;

import org.voovan.tools.TEnv;
import org.voovan.tools.TPerformance;
import org.voovan.tools.log.Logger;

import java.util.Timer;
import java.util.TimerTask;
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
public class ThreadPoolTask extends TimerTask {
	private Timer				timer;
	private ThreadPoolExecutor	threadPoolInstance;
	private int					cpuCoreCount;

	public ThreadPoolTask(ThreadPoolExecutor threadPoolInstance, Timer timer) {
		cpuCoreCount = Runtime.getRuntime().availableProcessors();
		this.timer = timer;
		this.threadPoolInstance = threadPoolInstance;
	}

	@Override
	public void run() {
		Thread mainThread = TEnv.getMainThread();

		if (threadPoolInstance.isShutdown()) {
			this.cancel();
			timer.cancel();
		}
		
//		String threadPoolInfo = "PoolInfo:" + threadPoolInstance.getActiveCount() + "/" + threadPoolInstance.getCorePoolSize() + "/"
//				+ threadPoolInstance.getLargestPoolSize() + " TaskCount: " + threadPoolInstance.getCompletedTaskCount() + "/"
//				+ threadPoolInstance.getTaskCount() + " QueueSize:" + threadPoolInstance.getQueue().size() + " PerCoreLoadAvg:"
//				+ TPerformance.cpuPerCoreLoadAvg();
//		if (threadPoolInstance.getActiveCount() != 0) {
//			Logger.simple(TDateTime.now() + " ShutDown:" + threadPoolInstance.isShutdown() + " " + threadPoolInfo);
//		}

		int poolSize = threadPoolInstance.getPoolSize();
		// 动态调整线程数,线程数要小于CPU核心数*100,且系统CPU负载值要小于1
		if (threadPoolInstance.getQueue().size() > 0 &&
				poolSize < cpuCoreCount * 50 &&
				TPerformance.cpuPerCoreLoadAvg() < 1) {
			threadPoolInstance.setCorePoolSize(threadPoolInstance.getPoolSize() + cpuCoreCount * 2);
			Logger.debug("PoolSizeChange: " + poolSize + "->" + threadPoolInstance.getCorePoolSize());
		}

		//如果主线程结束,则线程池也关闭
		if(mainThread!=null && mainThread.getState() == Thread.State.TERMINATED) {
			threadPoolInstance.shutdown();
		}

		//如果主线程没有的,则线程池也关闭
		if(mainThread==null){
			threadPoolInstance.shutdown();
		}
	}
}
