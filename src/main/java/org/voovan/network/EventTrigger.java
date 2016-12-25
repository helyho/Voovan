package org.voovan.network;

import org.voovan.Global;
import org.voovan.network.Event.EventName;

import java.nio.ByteBuffer;
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
		fireEventThread(session,EventName.ON_ACCEPTED,null);
	}
	
	public static void fireConnectThread(IoSession session){
		fireEventThread(session, EventName.ON_CONNECT,null);
	}
	
	public static void fireReceiveThread(IoSession session){
		// 当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		// 所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		// !hasEventDisposeing(EventName.ON_CONNECT) &&
		if (session.isConnect() && isHandShakeDone(session) && !session.isOnReceive()) {
			fireEventThread(session,EventName.ON_RECEIVE, null);
		}
	}
	
	public static void fireSentThread(IoSession session, ByteBuffer buffer){
		fireEventThread(session, EventName.ON_SENT,buffer);
	}

	public static void fireDisconnectThread(IoSession session){
		fireEventThread(session, EventName.ON_DISCONNECT,null);
	}

	public static void fireExceptionThread(IoSession session,Exception exception){
		fireEventThread(session,EventName.ON_EXCEPTION,exception);
	}
	
	public static void fireAccept(IoSession session){
		fireEvent(session,EventName.ON_ACCEPTED,null);
	}

	public static void fireConnect(IoSession session){
		fireEvent(session, EventName.ON_CONNECT,null);
	}
	
	public static void fireReceive(IoSession session){
		//当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		//所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		if (session.isConnect() && isHandShakeDone(session) && !session.isOnReceive()) {
			fireEvent(session, EventName.ON_RECEIVE,null);
		}
	}
	
	public static void fireSent(IoSession session, ByteBuffer buffer){
		fireEvent(session, EventName.ON_SENT,buffer);
	}
	
	public static void fireDisconnect(IoSession session){
		fireEvent(session,EventName.ON_DISCONNECT,null);
	}
	
	public static void fireException(IoSession session,Exception exception){
		fireEvent(session, EventName.ON_EXCEPTION,exception);
	}

	public static boolean isShutdown(){
		return eventThreadPool.isShutdown();
	}
	
	public static void shutdown(){
		eventThreadPool.shutdown();
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
	public static void fireEventThread(IoSession session,Event.EventName name,Object other){
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
	public static void fireEvent(IoSession session,Event.EventName name,Object other){
		Event event = Event.getInstance(session,name,other);
		EventProcess.process(event);
	}

}
