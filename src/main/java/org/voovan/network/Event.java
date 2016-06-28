package org.voovan.network;

import org.voovan.tools.TObject;

import java.util.List;

/**
 * 事件对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Event {
	private IoSession session;
	private EventName name;
	private Object other;
	private EventState state;
	private List<Event> eventPool;
	
	/**
	 * 事件名称枚举
	 * @author helyho
	 *
	 */
	public enum EventName {
		ON_ACCEPTED,ON_CONNECT,ON_DISCONNECT,ON_RECEIVE,ON_SENT,ON_EXCEPTION
	}
	
	/**
	 * 事件名称枚举
	 * @author helyho
	 *
	 */
	public enum EventState {
		READY,DISPOSEING,FINISHED
	}
	
	/**
	 * 构造函数
	 * @param session  会话对象
	 * @param name		事件名
	 * @param other 附加对象
	 */
	public Event(IoSession session, List<Event> eventPool, EventName name,Object other){
		this.session = session;
		this.name = name;
		this.other = other;
		this.state = EventState.READY;
		this.eventPool = eventPool;
	}

	public IoSession getSession() {
		return session;
	}

	public void setSession(IoSession session) {
		this.session = session;
	}

	public EventName getName() {
		return name;
	}

	public void setName(EventName name) {
		this.name = name;
	}

	public EventState getState() {
		return state;
	}

	public void setState(EventState state) {
		this.state = state;
	}

	public Object getOther() {
		return other;
	}

	public void setOther(Object other) {
		this.other = other;
	}

	public void removeFromPool(){
		if(eventPool!=null) {
			eventPool.remove(this);
		}
	}
	
	@Override
	public int hashCode(){
		return session.hashCode()+name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj != null && obj instanceof Event){
			Event compareEvent = TObject.cast( obj );
			if(session!=null && name != null
					&& session.equals(compareEvent.getSession())
					&& name.equals(compareEvent.getName())) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * 活的新的实例
	 * @param session   Session 对象
	 * @param name      事件名称
	 * @param other     附属对象
     * @return   事件对象
     */
	public static Event getInstance(IoSession session, List<Event> eventPool, Event.EventName name,Object other){
		return new Event(session, eventPool, name, other);
	}
	
}
