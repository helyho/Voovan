package org.voovan.network;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 事件处理线程
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
	private long threadId = 0;

	/**
	 * 事件处理 Thread
	 *
	 */
	public EventRunner(){
	}

	public long getThreadId() {
		return threadId;
	}

	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}

	public Object attachment() {
		return attachment;
	}

	public void attachment(Object attachment) {
		this.attachment = attachment;
	}

	public void addEvent(Runnable runnable){
		eventQueue.add(runnable);
	}

	public LinkedBlockingQueue<Runnable> getEventQueue() {
		return eventQueue;
	}

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
