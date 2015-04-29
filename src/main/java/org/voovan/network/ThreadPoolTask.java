package org.voovan.network;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.voovan.tools.TDateTime;
import org.voovan.tools.TPerformance;
import org.voovan.tools.log.Logger;

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
		if (threadPoolInstance.isShutdown()) {
			this.cancel();
			timer.cancel();
		}
		String threadPoolInfo = "PoolInfo:" + threadPoolInstance.getActiveCount() + "/" + threadPoolInstance.getCorePoolSize() + "/"
				+ threadPoolInstance.getLargestPoolSize() + " TaskCount: " + threadPoolInstance.getCompletedTaskCount() + "/"
				+ threadPoolInstance.getTaskCount() + " QueueSize:" + threadPoolInstance.getQueue().size() + " PerCoreLoadAvg:"
				+ TPerformance.cpuPerCoreLoadAvg();
		if (threadPoolInstance.getActiveCount() != 0) {
			Logger.simple(TDateTime.now() + " ShutDown:" + threadPoolInstance.isShutdown() + " " + threadPoolInfo);
		}

		int oldPoolSize = threadPoolInstance.getPoolSize();
		// 动态调整线程数,线程数要小于CPU核心数*100,且系统CPU负载值要小于1
		if (threadPoolInstance.getQueue().size() > 0 && oldPoolSize < cpuCoreCount * 100 && TPerformance.cpuPerCoreLoadAvg() < 1) {

			threadPoolInstance.setCorePoolSize(threadPoolInstance.getPoolSize() + cpuCoreCount * 2);
			Logger.simple("PoolSizeChange: " + oldPoolSize + "->" + threadPoolInstance.getCorePoolSize());
		}
	}
}
