package org.voovan.network;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * 事件执行器
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventRunner {

	private PriorityBlockingQueue<EventTask> eventQueue = new PriorityBlockingQueue<EventTask>();
	private Object attachment;
	private Thread thread = null;

	/**
	 * 事件处理 Thread
	 *
	 */
	public EventRunner(){
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
	 * 添加新的事件任务
	 * @param runnable 新事件任务对象
	 */
	public void addEvent(Runnable runnable){
		eventQueue.add(EventTask.newInstance(runnable));
	}

	public void addEvent(int priority, Runnable runnable){
		eventQueue.add(EventTask.newInstance(priority, runnable));
	}

	/**
	 * 获取事件任务对象集合
	 * @return 事件任务对象集合
	 */
	public PriorityBlockingQueue<EventTask> getEventQueue() {
		return eventQueue;
	}

	/**
	 * 执行
	 */
	public void process() {
		while (true) {
			try {
				EventTask eventTask = eventQueue.take();
				Runnable runnable = eventTask.getRunnable();
				if(runnable!=null) {
					runnable.run();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static class EventTask implements Comparable{
		private int priority;
		private Runnable runnable;

		public EventTask(int priority, Runnable runnable) {
			this.priority = priority;
			this.runnable = runnable;
		}

		public int getPriority() {
			return priority;
		}

		public void setPriority(int priority) {
			this.priority = priority;
		}

		public Runnable getRunnable() {
			return runnable;
		}

		public void setRunnable(Runnable runnable) {
			this.runnable = runnable;
		}

		public static EventTask newInstance(int priority, Runnable runnable){
			return new EventTask(priority, runnable);
		}

		public static EventTask newInstance(Runnable runnable){
			return new EventTask(0, runnable);
		}


		@Override
		public int compareTo(Object o) {
			EventTask current=(EventTask)o;
			if(current.priority > this.priority){
				return 1;
			} else if(current.priority==priority){
				return 0;
			} else {
				return -1;
			}
		}
	}

}
