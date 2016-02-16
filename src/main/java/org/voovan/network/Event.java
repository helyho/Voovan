package org.voovan.network;

import org.voovan.tools.TObject;

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
	 * @param exception	异常对象
	 */
	public Event(IoSession session,EventName name,Object other){
		this.session = session;
		this.name = name;
		this.other = other;
		this.state = EventState.READY;
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
	
	@Override
	public int hashCode(){
		return session.hashCode()+name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj){
		if(obj instanceof Event){
			Event compareEvent = TObject.cast( obj );
			return session.equals(compareEvent.session) && compareEvent.getName().equals(name);
		}
		else {
			return false;
		}
	}
	
	public static Event getInstance(IoSession session,Event.EventName name,Object other){
		return new Event(session, name, other);
	}
	
}
