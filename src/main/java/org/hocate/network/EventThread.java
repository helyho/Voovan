package org.hocate.network;

/**
 * 事件处理线程
 * @author helyho
 *
 */
public class EventThread  implements Runnable{
	
	private Event event;
	
	/**
	 * 事件处理 Thread
	 * @param events 事件队列
	 */
	public EventThread(Event event){
		this.event = event;
	}
	
	/**
	 * 获取事件
	 * @return
	 */
	public Event getEvent(){
		return event;
	}

	@Override
	public void run() { 
		EventProcess.process(event);
		System.out.println(Thread.currentThread().getName()+"is out");
	}
	
}
