package org.voovan.network;

import org.voovan.Global;
import org.voovan.tools.log.Logger;

import java.util.concurrent.ThreadPoolExecutor;

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

	private static ThreadPoolExecutor eventThreadPool = Global.getThreadPool();

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
		// 当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		// 所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		// !hasEventDisposeing(EventName.ON_CONNECT) &&
		synchronized (session.getState().getReceiveLock()) {
			if (session.isOpen() && isHandShakeDone(session) && session.getState().receiveLock()) {
				//设置接受状态
				session.getState().setReceive(true);

				fireEventThread(session, Event.EventName.ON_RECEIVE, null);
			}
		}
	}

	public static void fireSentThread(IoSession session, Object obj){
		//设置发送状态
		session.getState().setSend(true);

		fireEventThread(session, Event.EventName.ON_SENT, obj);
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
		//当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		//所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		synchronized (session.getState().getReceiveLock()) {
			if (session.isOpen() && isHandShakeDone(session) && session.getState().receiveLock()) {
				//设置接受状态
				session.getState().setReceive(true);
				fireEventThread(session, Event.EventName.ON_RECEIVE, null);
			}
		}
	}

	public static void fireSent(IoSession session, Object obj){
		//设置发送状态
		session.getState().setSend(true);

		fireEvent(session, Event.EventName.ON_SENT, obj);
	}

	public static void fireDisconnect(IoSession session){
		//设置断开状态,Close是最终状态
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

	public static boolean isHandShakeDone(IoSession session){
		if(session==null || session.getSSLParser()==null){
			return true;
		}else{
			return session.getSSLParser().isHandShakeDone();
		}
	}

	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public static void fireEventThread(IoSession session, Event.EventName name, Object other){
		if(!eventThreadPool.isShutdown()){
			Event event = Event.getInstance(session,name,other);
			eventThreadPool.execute(new EventThread(event));
		}
	}

	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public static void fireEvent(IoSession session, Event.EventName name, Object other){
		Event event = Event.getInstance(session,name,other);
		EventProcess.process(event);
	}

}
