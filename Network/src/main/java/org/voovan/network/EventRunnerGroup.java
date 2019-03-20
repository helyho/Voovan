package org.voovan.network;

import org.voovan.tools.TPerformance;
import org.voovan.tools.threadpool.ThreadPool;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventRunnerGroup {

	public static ThreadPoolExecutor IO_THREAD_POOL = ThreadPool.createThreadPool("IO", TPerformance.getProcessorCount(), TPerformance.getProcessorCount(), 60*1000);
	public static EventRunnerGroup EVENT_THREAD_POOL = new EventRunnerGroup(TPerformance.getProcessorCount());

	private AtomicInteger indexAtom = new AtomicInteger();
	private EventRunner[] eventRunners;
	private volatile int size;

	public EventRunnerGroup(int size){
		this.size = size;
		eventRunners = new EventRunner[size];
		for(int i=0;i<size;i++){
			EventRunner eventRunner = new EventRunner();
			eventRunners[i] = eventRunner;
			IO_THREAD_POOL.execute(eventRunner);
		}
	}

	public EventRunner choseEventRunner(){
		int index = indexAtom.getAndUpdate((val) ->{
			int newVal = val + 1;
			return (size == newVal) ? 0 : newVal;
		});

		return eventRunners[index];
	}
}
