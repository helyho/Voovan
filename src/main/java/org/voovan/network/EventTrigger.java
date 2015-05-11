package org.voovan.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ThreadPoolExecutor;

import org.voovan.network.Event.EventName;
import org.voovan.network.Event.EventState;
import org.voovan.tools.threadpool.ThreadPool;

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
	
	private IoSession session;
	private ThreadPoolExecutor eventThreadPool;
	private List<Event> eventPool;
	
	/**
	 * 构造函数
	 * @param session
	 */
	public EventTrigger(IoSession session){
		this.session = session;
		eventThreadPool =ThreadPool.getThreadPool();
		eventPool = new Vector<Event>();
	}
	
	/**
	 * 无参数构造函数
	 */
	public EventTrigger(){
		eventThreadPool = ThreadPool.getThreadPool();
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		eventThreadPool.allowCoreThreadTimeOut(true);
	}
	
	public List<Event> getEventPool() {
		return eventPool;
	}

	public void fireAcceptThread(IoSession session){
		clearFinishedEvent();
		fireEventThread(session,EventName.ON_ACCEPTED,null);
	}
	
	public void fireConnectThread(){
		clearFinishedEvent();
		fireEventThread(EventName.ON_CONNECT,null);
	}
	
	public synchronized void fireReceiveThread(){
		clearFinishedEvent();
		// 当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		// 所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		// !hasEventDisposeing(EventName.ON_CONNECT) &&
		if (session.isConnect() && isHandShakeDone() && !hasEventUnfinished(EventName.ON_RECEIVE)) {
			fireEventThread(EventName.ON_RECEIVE, null);
		}
	}
	
	public void fireSentThread(ByteBuffer buffer){
		clearFinishedEvent();
		fireEventThread(EventName.ON_SENT,buffer);
	}
	
	public void fireDisconnectThread(){
		clearFinishedEvent();
		fireEventThread(EventName.ON_DISCONNECT,null);
	}
	
	public void fireExceptionThread(Exception exception){
		clearFinishedEvent();
		fireEventThread(EventName.ON_EXCEPTION,exception);
	}
	
	public void fireAccept(IoSession session){
		clearFinishedEvent();
		fireEvent(session,EventName.ON_ACCEPTED,null);
	}

	public void fireConnect(){
		clearFinishedEvent();
		fireEvent(EventName.ON_CONNECT,null);
	}
	
	public void fireReceive(){
		clearFinishedEvent();
		//当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		//所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		if (session.isConnect() && isHandShakeDone() && !hasEventUnfinished(EventName.ON_RECEIVE)) {
			fireEvent(EventName.ON_RECEIVE,null);
		}
	}
	
	public void fireSent(ByteBuffer buffer){
		clearFinishedEvent();
		fireEvent(EventName.ON_SENT,buffer);
	}
	
	public void fireDisconnect(){
		clearFinishedEvent();
		fireEvent(EventName.ON_DISCONNECT,null);
	}
	
	public void fireException(Exception exception){
		clearFinishedEvent();
		fireEvent(EventName.ON_EXCEPTION,exception);
	}
	
	
	public boolean isShutdown(){
		clearFinishedEvent();
		return eventThreadPool.isShutdown();
	}
	
	public void shutdown(){
		clearFinishedEvent();
		eventThreadPool.shutdown();
	}
	
	public boolean isHandShakeDone(){
		if(session.getSSLParser()==null || session.getSSLParser().isHandShakeDone()){
			return true;
		}else{
			return false;
		}
	}
	
	public IoSession getSession(){
		return session;
	}
	
	/**
	 * 判断有没有特定的事件在执行
	 * @param eventName  事件名
	 * @return
	 */
	public boolean hasEventUnfinished(EventName eventName){
		for(Event event : eventPool){
			if(event.getName() == eventName && event.getState() != EventState.FINISHED ){
				return true;
			}
		}
		return false;
	}
	
	public void clearFinishedEvent(){
		List<Event> finishedEvent = new ArrayList<Event>();
		synchronized (eventPool) {
			for(Event event : eventPool){
				if(event.getState() == EventState.FINISHED){
					finishedEvent.add(event);
				}
			}
			eventPool.removeAll(finishedEvent);
		}
		
	}
	
	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param exception
	 */
	public void fireEventThread(IoSession session,Event.EventName name,Object other){
		if(!eventThreadPool.isShutdown()){
			Event event = Event.getInstance(session,name,other);
			eventPool.add(event);
			eventThreadPool.execute(new EventThread(event));
		}
	}
	
	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param name     事件名称
	 * @param exception
	 */
	public void fireEventThread(Event.EventName name,Object other){
		fireEventThread(session, name,other);
	}
	
	
	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param session  当前连接会话
	 * @param name     事件名称
	 * @param exception
	 */
	public void fireEvent(IoSession session,Event.EventName name,Object other){
		Event event = Event.getInstance(session,name,other);
		EventProcess.process(event);
		eventPool.add(event);
	}
	
	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param name     事件名称
	 * @param exception
	 */
	public void fireEvent(Event.EventName name,Object other){
		fireEvent(session, name,other);
	}

}
