package org.voovan.network;

import java.util.concurrent.LinkedBlockingQueue;

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

	private LinkedBlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<Runnable>();
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
		eventQueue.add(runnable);
	}

	/**
	 * 获取事件任务对象集合
	 * @return 事件任务对象集合
	 */
	public LinkedBlockingQueue<Runnable> getEventQueue() {
		return eventQueue;
	}

	/**
	 * 执行
	 */
	public void process() {
		while (true) {
			try {
				Runnable runnable = eventQueue.take();
				if(runnable!=null) {
					runnable.run();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
