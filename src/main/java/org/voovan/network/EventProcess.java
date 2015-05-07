package org.voovan.network;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.voovan.network.Event.EventName;
import org.voovan.network.Event.EventState;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.TObject;

/**
 * 事件的实际逻辑处理
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventProcess {

	/**
	 * 私有构造函数,防止被实例化
	 */
	private EventProcess(){
		
	}
	
	/**
	 * Accept事件
	 * 
	 * @param event
	 *            事件对象
	 * @throws Exception 
	 * @throws IOException
	 */
	public static void onAccepted(Event event) throws IOException {
		SocketContext socketContext = event.getSession().sockContext();
		if (socketContext != null) {
			socketContext.start();
		}
	}

	/**
	 * 连接成功事件 建立连接完成后出发
	 * 
	 * @param event
	 *            事件对象
	 * @throws SendMessageException 
	 * @throws IOException
	 */
	public static void onConnect(Event event) throws SendMessageException, IOException  {

		IoSession session = event.getSession();
		
		// SSL 握手
		if (session.getSSLParser() != null && !session.getSSLParser().isHandShakeDone()) {
			try {
				session.getSSLParser().doHandShake();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		SocketContext socketContext = event.getSession().sockContext();
		if (socketContext != null && session != null) {
			Object result = socketContext.handler().onConnect(session);
			if (result != null) {
				socketContext.filterChain().rewind();
				while (socketContext.filterChain().hasNext()) {
					IoFilter fitler = socketContext.filterChain().next();
					result = fitler.encode(session, result);
				}
				sendMessage(session, result);
			}
		}
	}

	/**
	 * 连接断开事件 断开后出发
	 * 
	 * @param event
	 *            事件对象
	 */
	public static void onDisconnect(Event event) {
		SocketContext socketContext = event.getSession().sockContext();
		if (socketContext != null) {
			IoSession session = event.getSession();

			socketContext.handler().onDisconnect(session);
		}
	}

	/**
	 * 读取事件 在消息接受完成后触发
	 * 
	 * @param event
	 *            事件对象
	 * @throws IOException 
	 * @throws SendMessageException 
	 * @throws Exception
	 */
	public static void onRead(Event event) throws IOException, SendMessageException {
		SocketContext socketContext = event.getSession().sockContext();
		IoSession session = event.getSession();
		if (socketContext != null && session != null) {
			
			ByteBuffer byteBuffer = ByteBuffer.allocate(1);
			
			//循环读取完整的消息包,由于之前有消息分割器在工作,所以这里读取的消息都是完成的消息包.
			//按消息包出发 onRecive 事件
			while (byteBuffer.limit() != 0) {
				
				byteBuffer = session.getMessageLoader().read();
				
				// 如果读出的消息为 null 则关闭连接
				if (byteBuffer == null) {
					session.close();
					return;
				}
				
				// 如果读出的数据长度为0,不触发事件
				if (byteBuffer.limit() == 0) {
					return;
				}

				// -----------------Filter 解密处理-----------------
				Object result = byteBuffer;

				// 取得过滤器链
				Chain<IoFilter> filterChain = socketContext.filterChain().clone();
				while (filterChain.hasNext()) {
					IoFilter fitler = filterChain.next();
					result = fitler.decode(session, result);
				}
				// -------------------------------------------------

				// -----------------Handler 业务处理-----------------
				if (result != null) {
					IoHandler handler = socketContext.handler();
					result = handler.onReceive(session, result);
				}
				// --------------------------------------------------

				// 返回的结果不为空的时候才发送
				if (result != null) {
					// ------------------Filter 加密处理-----------------
					filterChain.rewind();
					while (filterChain.hasNext()) {
						IoFilter fitler = filterChain.next();
						result = fitler.encode(session, result);
					}
					// ---------------------------------------------------

					// 发送消息
					if (result != null) {
						sendMessage(session, result);
					}
				}

				filterChain.clear();
			}
		}
	}

	/**
	 * 发送完成事件 发送后出发
	 * 
	 * @param event
	 *            事件对象
	 * @param obj
	 *            发送的对象
	 * @throws IOException
	 */
	public static void onSent(Event event, Object obj) throws IOException {
		SocketContext socketContext = event.getSession().sockContext();
		if (socketContext != null) {
			IoSession session = event.getSession();
			socketContext.handler().onSent(session, obj);
		}
	}

	/**
	 * 异常产生事件 异常产生侯触发
	 * 
	 * @param event
	 *            事件对象
	 * @param e
	 */
	public static void onException(Event event, Exception e) {
		if (event != null 
				&& event.getSession() != null 
				&& event.getSession().sockContext() != null) {
			SocketContext socketContext = event.getSession().sockContext();
			IoSession session = event.getSession();
			if (socketContext.handler() != null) {
				socketContext.handler().onException(session, e);
			}
		}else{
			e.printStackTrace();
		}
	}

	/**
	 * 消息发送
	 * 
	 * @param event
	 * @param sendBuf
	 * @throws SendMessageException 
	 * @throws IOException 
	 */
	public static void sendMessage(IoSession session, Object sendObj) throws SendMessageException, IOException{

		ByteBuffer resultBuf = null;
		// 根据消息类型,封装消息
		if (sendObj != null) {
			if (sendObj instanceof ByteBuffer) {
				resultBuf = TObject.cast(sendObj);
				resultBuf.rewind();
			} else if (sendObj instanceof String) {
				String sendString = TObject.cast(sendObj);
				resultBuf = ByteBuffer.wrap(sendString.getBytes());
			} else {
				throw new SendMessageException("Expect Object type is 'java.nio.ByteBuffer' or 'java.lang.String',reality got type is '"
						+ sendObj.getClass() + "'");
			}
		}

		// 发送消息
		if (sendObj != null && session.isConnect()) {

			if (session.getSSLParser() != null && session.getSSLParser().handShakeDone) {
				session.sendSSLData(resultBuf);
			} else {
				session.send(resultBuf);
			}

			Event event = new Event(session, EventName.ON_SENT, resultBuf);
			// 出发发送事件
			EventProcess.process(event);
		}
	}

	public static void process(Event event) {
		if (event == null) {
			return;
		}
		event.setState(EventState.DISPOSEING);
		EventName eventName = event.getName();
		// 根据事件名称处理事件
		try {
			if (eventName == EventName.ON_ACCEPTED) {
				SocketContext socketContext = TObject.cast(event.getSession().sockContext());
				socketContext.start();
			} else if (eventName == EventName.ON_CONNECT) {
				EventProcess.onConnect(event);
			} else if (eventName == EventName.ON_DISCONNECT) {
				EventProcess.onDisconnect(event);
			} else if (eventName == EventName.ON_RECEIVE) {
				EventProcess.onRead(event);
			} else if (eventName == EventName.ON_SENT) {
				EventProcess.onSent(event, event.getOther());
			} else if (eventName == EventName.ON_EXCEPTION) {
				EventProcess.onException(event, TObject.cast(event.getOther()));
			}
		} catch (Exception e) {
			EventProcess.onException(event, e);
		} finally {
			event.setState(EventState.FINISHED);
		}
	}
}
