package org.voovan.network;

import org.voovan.Global;
import org.voovan.tools.TPerformance;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventThreadPool {

	public static EventThreadPool EVENT_THREAD_POOL = new EventThreadPool(TPerformance.getProcessorCount());

	private AtomicInteger indexAtom = new AtomicInteger();
	private EventThread[] eventThreads;
	private volatile int size;

	public EventThreadPool(int size){
		this.size = size;
		eventThreads = new EventThread[size];
		for(int i=0;i<size;i++){
			EventThread eventThread = new EventThread();
			eventThreads[i] = eventThread;
			Global.getThreadPool().execute(eventThread);
		}
	}

	public EventThread choseEventThread(){
		int index = indexAtom.getAndUpdate((val) ->{
			int newVal = val + 1;
			return (size == newVal) ? 0 : newVal;
		});

		return eventThreads[index];
	}
}
