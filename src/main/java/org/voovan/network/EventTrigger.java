package org.voovan.network;

import org.voovan.Global;
import org.voovan.network.Event.EventName;
import org.voovan.network.Event.EventState;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;
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
	
	private ThreadPoolExecutor eventThreadPool;
	private List<Event> eventPool;

	/**
	 * 无参数构造函数
	 */
	public EventTrigger(){
		eventThreadPool = Global.getThreadPool();
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		eventThreadPool.allowCoreThreadTimeOut(true);
		eventPool = new Vector<Event>();
	}

	/**
	 * 获取事件池
	 * @return 事件集合
     */
	public List<Event> getEventPool() {
		return eventPool;
	}

	public void fireAcceptThread(IoSession session){
		fireEventThread(session,EventName.ON_ACCEPTED,null);
	}
	
	public void fireConnectThread(IoSession session){
		fireEventThread(session, EventName.ON_CONNECT,null);
	}
	
	public void fireReceiveThread(IoSession session){
		// 当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		// 所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		// !hasEventDisposeing(EventName.ON_CONNECT) &&
		if (session.isConnect() && isHandShakeDone(session) && !hasEventUnfinished(EventName.ON_RECEIVE)) {
			fireEventThread(session,EventName.ON_RECEIVE, null);
		}
	}
	
	public void fireSentThread(IoSession session, ByteBuffer buffer){
		fireEventThread(session, EventName.ON_SENT,buffer);
	}

	public void fireDisconnectThread(IoSession session){
		fireEventThread(session, EventName.ON_DISCONNECT,null);
	}

	public void fireExceptionThread(IoSession session,Exception exception){
		fireEventThread(session,EventName.ON_EXCEPTION,exception);
	}
	
	public void fireAccept(IoSession session){
		fireEvent(session,EventName.ON_ACCEPTED,null);
	}

	public void fireConnect(IoSession session){
		fireEvent(session, EventName.ON_CONNECT,null);
	}
	
	public void fireReceive(IoSession session){
		//当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		//所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		if (session.isConnect() && isHandShakeDone(session) && !hasEventUnfinished(EventName.ON_RECEIVE)) {
			fireEvent(session, EventName.ON_RECEIVE,null);
		}
	}
	
	public void fireSent(IoSession session, ByteBuffer buffer){
		fireEvent(session, EventName.ON_SENT,buffer);
	}
	
	public void fireDisconnect(IoSession session){
		fireEvent(session,EventName.ON_DISCONNECT,null);
	}
	
	public void fireException(IoSession session,Exception exception){
		fireEvent(session, EventName.ON_EXCEPTION,exception);
	}
	
	
	public boolean isShutdown(){
//		clearFinishedEvent();
		return eventThreadPool.isShutdown();
	}
	
	public void shutdown(){
		eventThreadPool.shutdown();
	}
	
	public boolean isHandShakeDone(IoSession session){
		if(session==null || session.getSSLParser()==null){
			return true;
		}else{
			return session.getSSLParser().isHandShakeDone();
		}
	}

	/**
	 * 判断有没有特定的事件在执行
	 * @param eventName  事件名
	 * @return 事件是否在处理
	 */
	public boolean hasEventUnfinished(EventName eventName){
		for (Event event : eventPool) {
			if (event.getName() == eventName && event.getState() != EventState.FINISHED) {
				return true;
			}
		}
		return false;
	}

	
	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public void fireEventThread(IoSession session,Event.EventName name,Object other){
		if(!eventThreadPool.isShutdown()){
			Event event = Event.getInstance(session,eventPool,name,other);
			eventPool.add(event);
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
	public void fireEvent(IoSession session,Event.EventName name,Object other){
		Event event = Event.getInstance(session,eventPool,name,other);
		EventProcess.process(event);
		eventPool.add(event);
	}

}
