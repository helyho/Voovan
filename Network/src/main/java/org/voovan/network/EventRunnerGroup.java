package org.voovan.network;

import org.voovan.tools.TObject;
import org.voovan.tools.TPerformance;
import org.voovan.tools.TString;
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
	public static int ACCEPT_THREAD_SIZE = 1;
	public static ThreadPoolExecutor ACCEPT_THREAD_POOL = ThreadPool.createThreadPool("ACCEPT", 1, ACCEPT_THREAD_SIZE, 60*1000, true, 10);
	public static EventRunnerGroup ACCEPT_EVENT_RUNNER_GROUP= new EventRunnerGroup(ACCEPT_THREAD_POOL, ACCEPT_THREAD_SIZE, (obj)->{
		try {
			return new SocketSelector(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	});

	public static int IO_THREAD_SIZE = Integer.valueOf(TObject.nullDefault(System.getProperty("IoThreadSize"),TPerformance.getProcessorCount()+""));
	public static ThreadPoolExecutor IO_THREAD_POOL = ThreadPool.createThreadPool("IO", IO_THREAD_SIZE, IO_THREAD_SIZE, 60*1000, true, 9);
	public static EventRunnerGroup IO_EVENT_RUNNER_GROUP= new EventRunnerGroup(IO_THREAD_POOL, IO_THREAD_SIZE, (obj)->{
		try {
			return new SocketSelector(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	});

	static {
		System.out.println("[SOCKET] IO_THREAD_SIZE: " + IO_THREAD_SIZE);
	}


	private AtomicInteger indexAtom = new AtomicInteger();
	private EventRunner[] eventRunners;
	private volatile int size;

	/**
	 * 构造方法
	 * @param threadPoolExecutor 用于分发任务执行的线程池
	 * @param size 容纳事件执行器的数量
	 * @param attachmentSupplier 事件执行器的附属对象构造器
	 */
	public EventRunnerGroup(ThreadPoolExecutor threadPoolExecutor, int size, Function<EventRunner, Object> attachmentSupplier){
		this.size = size;
		eventRunners = new EventRunner[size];
		for(int i=0;i<size;i++){
			EventRunner eventRunner = new EventRunner();

			if(attachmentSupplier!=null) {
				eventRunner.attachment(attachmentSupplier.apply(eventRunner));
			}
			eventRunners[i] = eventRunner;

			threadPoolExecutor.execute(()->{
				eventRunner.setThread(Thread.currentThread());
				eventRunner.process();
			});
		}
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
}
