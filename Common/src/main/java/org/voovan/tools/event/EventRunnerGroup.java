package org.voovan.tools.event;

import org.voovan.tools.threadpool.ThreadPool;

import java.util.Queue;
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
	private AtomicInteger indexAtom = new AtomicInteger();
	private EventRunner[] eventRunners;
	private ThreadPoolExecutor threadPool;
	private int size;
	private volatile boolean isSteal = false;

	/**
	 * 构造方法
	 * @param threadPoolExecutor 用于分发任务执行的线程池
	 * @param size 容纳事件执行器的数量
	 * @param isSteal 是否允许任务窃取
	 * @param attachmentSupplier 事件执行器的附属对象构造器
	 */
	public EventRunnerGroup(ThreadPoolExecutor threadPoolExecutor, int size, boolean isSteal, Function<EventRunner, Object> attachmentSupplier) {
		this.size = size;
        this.threadPool = threadPoolExecutor;
        this.isSteal = isSteal;

		eventRunners = new EventRunner[size];
		for(int i=0;i<size;i++){
			EventRunner eventRunner = new EventRunner(this);

			if(attachmentSupplier!=null) {
				//构造事件执行器的服务对象
				eventRunner.attachment(attachmentSupplier.apply(eventRunner));
			}
			eventRunners[i] = eventRunner;

			//为事件执行器分配线程并启动
			eventRunner.process();
		}
	}

	public boolean isSteal() {
		return isSteal;
	}

	public void setSteal(boolean steal) {
		isSteal = steal;
	}

	public ThreadPoolExecutor getThreadPool() {
		return threadPool;
	}

	public EventRunner[] getEventRunners() {
		return eventRunners;
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

	/**
	 * 从任务最多的 EventRunner 窃取任务
	 * @return EventTask 对象
	 */
	public EventTask stealTask() {
		if(!isSteal) {
			return null;
		}

		EventRunner largestEventRunner = null;
		for(EventRunner eventRunner : eventRunners) {
			if(eventRunner == null) {
				largestEventRunner = eventRunner;
				continue;
			}

			if(eventRunner.getEventQueue().size() > largestEventRunner.getEventQueue().size()) {
				largestEventRunner = eventRunner;
				break;
			}
		}

		if(largestEventRunner == null) {
			return null;
		}

		Queue<EventTask> eventQueue = largestEventRunner.getEventQueue();
		synchronized (eventQueue) {
			return largestEventRunner.getEventQueue().poll();
		}

	}

	/**
	 * 静态构造方法
	 * @param groupName 事件执行器名称
	 * @param size 容纳事件执行器的数量
	 * @param isSteal 是否允许任务窃取
	 * @param threadPriority 线程优先级
	 * @param attachmentSupplier 事件执行器的附属对象构造器
	 * @return 事件执行管理器
	 */
	public static EventRunnerGroup newInstance(String groupName, int size, boolean isSteal, int threadPriority, Function<EventRunner, Object> attachmentSupplier){
		ThreadPoolExecutor threadPoolExecutor = ThreadPool.createThreadPool(groupName, size, size, 60*1000, true, threadPriority);
		return new EventRunnerGroup(threadPoolExecutor, size, isSteal, attachmentSupplier);
	}
}
