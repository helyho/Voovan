package org.hocate.network;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.hocate.log.Logger;
import org.hocate.tools.TDateTime;



/**
 * 线程池
 * @author helyho
 *
 */
public class ThreadPool {
	private static ThreadPoolExecutor threadPool = createThreadPool();
	public static Hashtable<String,Object> temp = new Hashtable<String, Object>();
	
	private static ThreadPoolExecutor createThreadPool(){
//		int cpuCoreCount = Runtime.getRuntime().availableProcessors();
		ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();//new ThreadPoolExecutor(cpuCoreCount*1000, cpuCoreCount*2000,1, TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
		//设置allowCoreThreadTimeOut,允许回收超时的线程
//		threadPool.allowCoreThreadTimeOut(true);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				String threadPoolInfo = "PoolInfo:"+threadPool.getActiveCount()+"/"+threadPool.getCorePoolSize()+" TaskCount: "
						+threadPool.getCompletedTaskCount()+"/"+threadPool.getTaskCount()+" QueueSize:"+threadPool.getQueue().size();
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
