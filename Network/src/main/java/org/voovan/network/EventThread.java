package org.voovan.network;

import org.voovan.network.nio.NioSelector;

import java.util.concurrent.ArrayBlockingQueue;

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

	public ArrayBlockingQueue<Event> eventQueue = new ArrayBlockingQueue<Event>(200000);

	/**
	 * 事件处理 Thread
	 *
	 */
	public EventThread(){
	}

	public void addEvent(Event event){
		eventQueue.add(event);
	}

	@Override
	public void run() {
		while (true) {
			try {
				Event event = eventQueue.take();
				if(event!=null) {
					EventProcess.process(event);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
