package org.voovan.tools.event;

import org.voovan.tools.exception.EventRunnerException;
import org.voovan.tools.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 事件执行器
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventRunner implements Closeable {

	private PriorityBlockingQueue<EventTask> eventQueue = new PriorityBlockingQueue<EventTask>();
	private Object attachment;
	private Thread thread = null;
	private EventRunnerGroup eventRunnerGroup;
	private boolean running = false;

	/**
	 * 事件处理 Thread
	 * @param eventRunnerGroup EventRunnerGroup对象
	 *
	 */
	public EventRunner(EventRunnerGroup eventRunnerGroup){
		this.eventRunnerGroup = eventRunnerGroup;
	}

	/**
	 * 获取绑定的线程
	 * @return 线程
	 */
	public Thread getThread() {
		return thread;
	}

	/**
	 * 设置绑定的线程
	 * @param thread 线程
	 */
	void setThread(Thread thread) {
		this.thread = thread;
	}

	/**
	 * 获取附属对象
	 * @return 附属对象
	 */
	public Object attachment() {
		return attachment;
	}

	/**
	 * 设置附属对象
	 * @param attachment 附属对象
	 */
	public void attachment(Object attachment) {
		this.attachment = attachment;
	}

	/**
	 * 添加事件
	 * @param priority 事件优先级必须在1-10之间
	 * @param runnable 事件执行器
	 */
	public void addEvent(int priority, Runnable runnable) {
		if(priority > 10 || priority < 1) {
			throw new EventRunnerException("priority must between 1-10");
		}
		eventQueue.add(EventTask.newInstance(priority, runnable));
	}

	/**
	 * 添加事件
	 * @param runnable 事件执行器
	 */
	public void addEvent(Runnable runnable){
		addEvent(5, runnable);
	}

	/**
	 * 获取事件任务对象集合
	 * @return 事件任务对象集合
	 */
	public PriorityBlockingQueue<EventTask> getEventQueue() {
		return eventQueue;
	}


	@Override
	public void close() {
		running = false;
	}

	/**
	 * 执行, 在 EventRunnerGroup 执行
	 */
	public void process() {
		if(running) {
		    return;
		}

		running = true;
		//启动线程任务执行
		eventRunnerGroup.getThreadPool().execute(()->{
			this.setThread(Thread.currentThread());
			while (running) {
				try {
					EventTask eventTask = eventQueue.poll(1000, TimeUnit.MILLISECONDS);

					//窃取任务
					if(eventRunnerGroup.isSteal() && eventTask == null) {
						eventTask = eventRunnerGroup.stealTask();
						if(eventTask!=null) {
						}
					}

					if(eventTask!=null) {
						Runnable runnable = eventTask.getRunnable();
						if (runnable != null) {
							runnable.run();
						}
					} else {
						if(eventRunnerGroup.getThreadPool().isShutdown()){
							break;
						}
					}
				} catch (Throwable e) {
					Logger.error(e);
					continue;
				}
			}
		});
	}

}
