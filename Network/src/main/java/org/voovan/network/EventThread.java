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
public class EventThread implements Runnable{

	private LinkedBlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<Runnable>();

	/**
	 * 事件处理 Thread
	 *
	 */
	public EventThread(){
	}

	public void addEvent(Runnable runnable){
		eventQueue.add(runnable);
	}

	public LinkedBlockingQueue<Runnable> getEventQueue() {
		return eventQueue;
	}

	@Override
	public void run() {
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
