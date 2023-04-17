package org.voovan.tools.event;

import org.voovan.tools.TEnv;
import org.voovan.tools.TPerformance;
import org.voovan.tools.threadpool.ThreadPool;

import java.io.Closeable;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 事件执行管理器
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventRunnerGroup implements Closeable {
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
	 * 启动 EventRunnerGroup 中所有的 EventRunner
	 * @return EventRunnerGroup 对象
	 */
	public EventRunnerGroup process() {
		for(EventRunner eventRunner : eventRunners) {
			eventRunner.process();
		}

		return this;
	}

	private EventRunner eventRunnerSelector( Supplier<Integer> selector) {
		if(selector == null) {
			throw new NullPointerException("EventRunnerGroup.addEvent parameter 'selector==null'");
		}
		Integer index = selector.get();
		if(index == null) {
			throw new NullPointerException("EventRunnerGroup.addEvent try invoke 'selector.get()==null'");
		}
		if(index > eventRunners.length) {
			index = index%eventRunners.length;
		}

		return eventRunners[index];
	}


	/**
	 * 添加事件
	 * @param priority 事件优先级必须在1-10之间
	 * @param runnable 事件执行器
	 * @Param selector 自定义执行器的选择
	 */
	public void addEvent(int priority, Runnable runnable, Supplier<Integer> selector) {
		eventRunnerSelector(selector).addEvent(priority, runnable);
	}

	/**
	 * 添加事件
	 * @param priority 事件优先级必须在1-10之间
	 * @param runnable 事件执行器
	 */
	public void addEvent(int priority, Runnable runnable) {
		choseEventRunner().addEvent(priority, runnable);
	}

	/**
	 * 添加事件
	 * @param runnable 事件执行器
	 */
	public void addEvent(Runnable runnable){
		addEvent(5, runnable);
	}

	/**
	 * 添加事件
	 * @param priority 事件优先级必须在1-10之间
	 * @param callable 事件执行器
	 * @Param selector 自定义执行器的选择
	 * @return Future 对象
	 */
	public <V> Future<V> addEvent(int priority, Callable<V> callable, Supplier<Integer> selector) {
		return eventRunnerSelector(selector).addEvent(priority, callable);
	}

	/**
	 * 添加事件
	 * @param priority 事件优先级必须在1-10之间
	 * @param callable 事件执行器
	 * @return Future 对象
	 */
	public <V> Future<V> addEvent(int priority, Callable callable) {
		return choseEventRunner().addEvent(priority, callable);
	}

	/**
	 * 添加事件
	 * @param runnable 事件执行器
	 * @return Future 对象
	 */
	public <V> Future<V> addEvent(Callable callable){
		return addEvent(5, callable);
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
			if(largestEventRunner == null) {
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
	 * 启动 EventRunnerGroup 中所有的 EventRunner, 并等待所有 EventRunner 中的任务执行完成
	 * @return EventRunnerGroup 对象
	 */
	public EventRunnerGroup await() {
		process();
		for(;;) {
			int count = 0;
			for (EventRunner eventRunner : eventRunners) {
				if(eventRunner.getThread().getState() == Thread.State.RUNNABLE) {
					count++;
					break;
				} else {
					count += eventRunner.getEventQueue().size();
				}
			}

			if(count>0) {
				TEnv.sleep(1);
				continue;
			} else {
				break;
			}
		}

		return this;
	}

	/**
	 * 放弃 EventRunner 中未执行的任务, 等带当前正在执行的任务完成后关闭
	 */
	@Override
	public void close() {
		for(EventRunner eventRunner : eventRunners) {
			eventRunner.close();
		}

		//关闭线程池
		ThreadPool.gracefulShutdown(this.getThreadPool());
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


	public static EventRunnerGroup newInstance(int size, boolean isSteal) {
		ThreadPoolExecutor threadPoolExecutor  = ThreadPool.createThreadPool("ERG", size, size , 60*1000);
		return new EventRunnerGroup(threadPoolExecutor, threadPoolExecutor.getMaximumPoolSize(),isSteal, null);
	}

	public static EventRunnerGroup newInstance(int size) {
		return EventRunnerGroup.newInstance(size, true);
	}

	public static EventRunnerGroup newInstance() {
		return EventRunnerGroup.newInstance(TPerformance.getProcessorCount(), true);
	}

}
