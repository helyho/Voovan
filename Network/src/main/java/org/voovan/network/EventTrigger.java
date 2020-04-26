package org.voovan.network;

/**
 * 事件触发器
 *
 * 		触发各种事件
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventTrigger {

	public static void fireAcceptAsync(IoSession session){
		fireEvent(session, Event.EventName.ON_ACCEPTED,null);
	}

	public static void fireConnectAsync(IoSession session){
		//设置连接状态
		session.getState().setInit(false);
		session.getState().setConnect(true);

		fireEvent(session, Event.EventName.ON_CONNECT,null);
	}

	public static void fireReceiveAsync(IoSession session) {
		fireEvent(session, Event.EventName.ON_RECEIVE, null);
	}

	public static void fireSentAsync(IoSession session, Object obj){
		fireEvent(session, Event.EventName.ON_SENT, obj);
	}


	public static void fireFlushAsync(IoSession session){
		fireEvent(session, Event.EventName.ON_FLUSH, null);
	}

	public static void fireDisconnectAsync(IoSession session){
		//设置断开状态,Close是最终状态
		session.getState().setClose(true);
		fireEvent(session, Event.EventName.ON_DISCONNECT, null);
	}

	public static void fireIdleAsync(IoSession session){
		if(session.getIdleInterval() >0 ) {
			fireEvent(session, Event.EventName.ON_IDLE, null);
		}
	}

	public static void fireExceptionAsync(IoSession session,Exception exception){
		fireEvent(session, Event.EventName.ON_EXCEPTION,exception);
	}

	public static void fireAccept(IoSession session){
		fire(session, Event.EventName.ON_ACCEPTED,null);
	}

	public static void fireConnect(IoSession session){
		//设置连接状态
		session.getState().setInit(false);
		session.getState().setConnect(true);

		fire(session, Event.EventName.ON_CONNECT,null);
	}

	public static void fireReceive(IoSession session){
		fire(session, Event.EventName.ON_RECEIVE, null);
	}

	public static void fireSent(IoSession session, Object obj){
		fire(session, Event.EventName.ON_SENT, obj);
	}

	public static void fireFlush(IoSession session){
		fire(session, Event.EventName.ON_FLUSH, null);
	}

	public static void fireDisconnect(IoSession session){
		session.getState().setClose(true);

		fire(session, Event.EventName.ON_DISCONNECT,null);
	}

	public static void fireIdle(IoSession session){
		if(session.getIdleInterval() >0 ) {
			fire(session, Event.EventName.ON_IDLE, null);
		}
	}

	public static void fireException(IoSession session,Exception exception){
		fire(session, Event.EventName.ON_EXCEPTION,exception);
	}

	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public static void fireEvent(IoSession session, Event.EventName name, Object other){
        session.getEventRunner().addEvent(5, ()->{
				Event event = new Event(session, name, other);
				EventProcess.process(event);
			});
	}

	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public static void fire(IoSession session, Event.EventName name, Object other){
		Event event = new Event(session, name, other);
		EventProcess.process(event);
	}
}
