package org.hocate.network;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hocate.tools.log.Logger;
import org.hocate.tools.TDateTime;
import org.hocate.tools.TPerformance;



/**
 * 线程池
 * @author helyho
 *
 */
public class ThreadPool {
	private static ThreadPoolExecutor threadPool = createThreadPool();
	public static Hashtable<String,Object> temp = new Hashtable<String, Object>();
	
	private static ThreadPoolExecutor createThreadPool(){
		int cpuCoreCount = Runtime.getRuntime().availableProcessors();
		ThreadPoolExecutor threadPool = new ThreadPoolExecutor(cpuCoreCount*10, cpuCoreCount*10,1, TimeUnit.MINUTES,new LinkedBlockingQueue<Runnable>());
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		threadPool.allowCoreThreadTimeOut(true);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(threadPool.isShutdown()){
					this.cancel();
					timer.cancel();
				}
				String threadPoolInfo = "PoolInfo:"+threadPool.getActiveCount()+"/"+threadPool.getCorePoolSize()+"/"+threadPool.getLargestPoolSize()+" TaskCount: "
						+threadPool.getCompletedTaskCount()+"/"+threadPool.getTaskCount()+" QueueSize:"+threadPool.getQueue().size()+" PerCoreLoadAvg:"+TPerformance.cpuPerCoreLoadAvg();
				if(threadPool.getActiveCount()!=0){
					Logger.simple(TDateTime.now()+" ShutDown:"+threadPool.isShutdown()+" "+threadPoolInfo);
				}
				
				int oldPoolSize = threadPool.getPoolSize();
				//动态调整线程数,线程数要小于CPU核心数*100,且系统CPU负载值要小于1
				if(threadPool.getQueue().size()>0 && oldPoolSize<cpuCoreCount*100 && TPerformance.cpuPerCoreLoadAvg()<1){

					threadPool.setCorePoolSize(threadPool.getPoolSize()+cpuCoreCount*2);
					Logger.simple("PoolSizeChange: "+oldPoolSize+"->"+threadPool.getCorePoolSize());
				}
			}
		}, 1, 500);
		
		
		return threadPool;
	}
	
	public static ThreadPoolExecutor getThreadPool(){
		return threadPool;
	}
}
