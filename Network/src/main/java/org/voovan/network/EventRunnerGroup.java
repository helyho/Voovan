package org.voovan.network;

import org.voovan.tools.TPerformance;
import org.voovan.tools.threadpool.ThreadPool;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 事件执行管理器
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventRunnerGroup {

	public static ThreadPoolExecutor IO_THREAD_POOL = ThreadPool.createThreadPool("IO", TPerformance.getProcessorCount(), TPerformance.getProcessorCount(), 60*1000);
	public static EventRunnerGroup EVENT_RUNNER_GROUP= new EventRunnerGroup(TPerformance.getProcessorCount(), (obj)->{
		try {
			return new SocketSelector(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	});

	private AtomicInteger indexAtom = new AtomicInteger();
	private EventRunner[] eventRunners;
	private volatile int size;

	/**
	 * 构造方法
	 * @param size 容纳事件执行器的数量
	 * @param attachmentSupplier 事件执行器的附属对象构造器
	 */
	public EventRunnerGroup(int size, Function<EventRunner, Object> attachmentSupplier){
		this.size = size;
		eventRunners = new EventRunner[size];
		for(int i=0;i<size;i++){
			EventRunner eventRunner = new EventRunner();

			if(attachmentSupplier!=null) {
				eventRunner.attachment(attachmentSupplier.apply(eventRunner));
			}
			eventRunners[i] = eventRunner;

			IO_THREAD_POOL.execute(()->{
				eventRunner.setThread(Thread.currentThread());
				eventRunner.process();
			});
		}
	}

	/**
	 * 选择一个时间执行器
	 * @return 事件执行器对象
	 */
	public EventRunner choseEventRunner(){
		//TODO: 这里最好能够使用负载均衡算法
		int index = indexAtom.getAndUpdate((val) ->{
			int newVal = val + 1;
			return (size == newVal) ? 0 : newVal;
		});


		EventRunner eventRunner = eventRunners[index];
		return eventRunner;
	}
}
