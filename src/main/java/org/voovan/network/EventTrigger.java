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
	
	private IoSession session;
	private ThreadPoolExecutor eventThreadPool;
	private List<Event> eventPool;
	
	/**
	 * 构造函数
	 * @param session Session 对象
	 */
	public EventTrigger(IoSession session){
		this.session = session;
		eventThreadPool = Global.getThreadPool();
		eventPool = new Vector<Event>();
	}
	
	/**
	 * 无参数构造函数
	 */
	public EventTrigger(){
		eventThreadPool = Global.getThreadPool();
		//设置allowCoreThreadTimeOut,允许回收超时的线程
		eventThreadPool.allowCoreThreadTimeOut(true);
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
	
	public void fireConnectThread(){
		fireEventThread(EventName.ON_CONNECT,null);
	}
	
	public void fireReceiveThread(){
		// 当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		// 所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		// !hasEventDisposeing(EventName.ON_CONNECT) &&
		if (session.isConnect() && isHandShakeDone() && !hasEventUnfinished(EventName.ON_RECEIVE)) {
			fireEventThread(EventName.ON_RECEIVE, null);
		}
	}

	public void fireReceiveThread(IoSession session){
		// 当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		// 所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		// !hasEventDisposeing(EventName.ON_CONNECT) &&
		if (session.isConnect() && isHandShakeDone() && !hasEventUnfinished(EventName.ON_RECEIVE)) {
			fireEventThread(session,EventName.ON_RECEIVE, null);
		}
	}
	
	public void fireSentThread(ByteBuffer buffer){
		fireEventThread(EventName.ON_SENT,buffer);
	}
	
	public void fireDisconnectThread(){
		fireEventThread(EventName.ON_DISCONNECT,null);
	}
	
	public void fireExceptionThread(Exception exception){
		fireEventThread(EventName.ON_EXCEPTION,exception);
	}

	public void fireExceptionThread(IoSession session,Exception exception){
		fireEventThread(session,EventName.ON_EXCEPTION,exception);
	}
	
	public void fireAccept(IoSession session){
		fireEvent(session,EventName.ON_ACCEPTED,null);
	}

	public void fireConnect(){
		fireEvent(EventName.ON_CONNECT,null);
	}
	
	public void fireReceive(){
		//当消息长度大于缓冲区时,receive 会在缓冲区满了后就出发,这时消息还没有发送完,会被触发多次
		//所以当有 receive 事件正在执行则抛弃后面的所有 receive 事件
		if (session.isConnect() && isHandShakeDone() && !hasEventUnfinished(EventName.ON_RECEIVE)) {
			fireEvent(EventName.ON_RECEIVE,null);
		}
	}
	
	public void fireSent(ByteBuffer buffer){
		fireEvent(EventName.ON_SENT,buffer);
	}
	
	public void fireDisconnect(){
		fireEvent(EventName.ON_DISCONNECT,null);
	}
	
	public void fireException(Exception exception){
		fireEvent(EventName.ON_EXCEPTION,exception);
	}
	
	
	public boolean isShutdown(){
//		clearFinishedEvent();
		return eventThreadPool.isShutdown();
	}
	
	public void shutdown(){
		eventThreadPool.shutdown();
	}
	
	public boolean isHandShakeDone(){
		if(session==null || session.getSSLParser()==null){
			return true;
		}else{
			return session.getSSLParser().isHandShakeDone();
		}
	}
	
	public IoSession getSession(){
		return session;
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
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public void fireEventThread(Event.EventName name,Object other){
		fireEventThread(session, name,other);
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
	
	/**
	 * 事件触发
	 * 		根据事件启动 EventThread 来处理事件
	 * @param name     事件名称
	 * @param other 附属对象
	 */
	public void fireEvent(Event.EventName name,Object other){
		fireEvent(session, name,other);
	}

}
