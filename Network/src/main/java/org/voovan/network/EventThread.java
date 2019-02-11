package org.voovan.network;

/**
 * 事件处理线程
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventThread  implements Runnable{
	public static ThreadLocal<Event> THREAD_EVENT = ThreadLocal.withInitial(()->new Event());

	private IoSession session;
	private Event.EventName name;
	private Object other;

	/**
	 * 事件处理 Thread
	 *
	 */
	public EventThread(){
	}

	/**
	 * 事件处理 Thread
	 *
	 * @param session IoSession对象
	 * @param name  事件名称
	 * @param other 附属对象
	 */
	public EventThread(IoSession session, Event.EventName name, Object other){
		init(session, name, other);
	}

	public void init(IoSession session, Event.EventName name, Object other){
		this.session = session;
		this.name = name;
		this.other = other;
	}

	@Override
	public void run() {
		Event event = THREAD_EVENT.get();
		event.init(session, name, other);

		EventProcess.process(event);
	}

}
