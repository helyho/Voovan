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

	public static void fireAcceptThread(IoSession session){
		fireEventThread(session, Event.EventName.ON_ACCEPTED,null);
	}

	public static void fireConnectThread(IoSession session){
		//设置连接状态
		session.getState().setConnect(true);
		session.getState().setInit(false);

		fireEventThread(session, Event.EventName.ON_CONNECT,null);
	}

	public static void fireReceiveThread(IoSession session){
		fireEventThread(session, Event.EventName.ON_RECEIVE, null);
	}

	public static void fireSentThread(IoSession session, Object obj){
		fireEventThread(session, Event.EventName.ON_SENT, obj);
	}


	public static void fireFlushThread(IoSession session){
		fireEventThread(session, Event.EventName.ON_FLUSH, null);
	}

	public static void fireDisconnectThread(IoSession session){
		//设置断开状态,Close是最终状态
		session.getState().setClose(true);

		fireEventThread(session, Event.EventName.ON_DISCONNECT, null);
	}

	public static void fireIdleThread(IoSession session){
		if(session.getIdleInterval() >0 ) {
			fireEventThread(session, Event.EventName.ON_IDLE, null);
		}
	}

	public static void fireExceptionThread(IoSession session,Exception exception){
		fireEventThread(session, Event.EventName.ON_EXCEPTION,exception);
	}

	public static void fireAccept(IoSession session){
		fireEvent(session, Event.EventName.ON_ACCEPTED,null);
	}

	public static void fireConnect(IoSession session){
		//设置连接状态
		session.getState().setConnect(true);
		session.getState().setInit(false);

		fireEvent(session, Event.EventName.ON_CONNECT,null);
	}

	public static void fireReceive(IoSession session){
		fireEvent(session, Event.EventName.ON_RECEIVE, null);
	}

	public static void fireSent(IoSession session, Object obj){
		fireEvent(session, Event.EventName.ON_SENT, obj);
	}

	public static void fireFlush(IoSession session){
		fireEvent(session, Event.EventName.ON_FLUSH, null);
	}

	public static void fireDisconnect(IoSession session){
		session.getState().setClose(true);

		fireEvent(session, Event.EventName.ON_DISCONNECT,null);
	}

	public static void fireIdle(IoSession session){
		if(session.getIdleInterval() >0 ) {
			fireEvent(session, Event.EventName.ON_IDLE, null);
		}
	}

	public static void fireException(IoSession session,Exception exception){
		fireEvent(session, Event.EventName.ON_EXCEPTION,exception);
	}

	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public static void fireEventThread(IoSession session, Event.EventName name, Object other){
        session.getEventRunner().addEvent(5, ()->{
				if(session.isConnected()) {
					Event event = new Event(session, name, other);
					EventProcess.process(event);
				}
			});
	}

	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public static void fireEvent(IoSession session, Event.EventName name, Object other){
		Event event = new Event(session,name,other);
		EventProcess.process(event);
	}

}
