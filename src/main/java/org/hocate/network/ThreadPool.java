package org.hocate.network;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hocate.log.Logger;
import org.hocate.tools.TDateTime;


/**
 * 线程池
 * @author helyho
 *
 */
public class ThreadPool {
	private static ThreadPoolExecutor threadPool = createThreadPool();
	
	private static ThreadPoolExecutor createThreadPool(){
		int cpuCoreCount = Runtime.getRuntime().availableProcessors();
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(cpuCoreCount*100, cpuCoreCount*2*100,1, TimeUnit.MILLISECONDS,new LinkedBlockingQueue<Runnable>());
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPool.allowCoreThreadTimeOut(true);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				String threadPoolInfo = "PoolInfo:"+threadPool.getActiveCount()+"/"+threadPool.getPoolSize()+" TaskCount: "+threadPool.getTaskCount()+" QueueSize:"+threadPool.getQueue().size();
				if(threadPool.getActiveCount()!=0 || threadPool.getPoolSize()!=0){
					Logger.simple(TDateTime.currentTime()+"-"+threadPool.isShutdown()+" "+threadPoolInfo);
				}
			}
		}, 1, 1000);
		return threadPool;
	}
	
	public static ThreadPoolExecutor getThreadPool(){
		return threadPool;
	}
}
