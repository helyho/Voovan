package org.voovan.network;

import org.voovan.network.Event.EventName;
import org.voovan.network.exception.IoFilterException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.Chain;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;

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
	 * @throws IOException IO 异常
	 */
	public static void onAccepted(Event event) throws IOException {
		SocketContext socketContext = event.getSession().socketContext();
		if (socketContext != null) {
			socketContext.start();
		}
	}

	/**
	 * 连接成功事件 建立连接完成后出发
	 * 
	 * @param event
	 *            事件对象
	 * @throws SendMessageException  消息发送异常
	 * @throws IOException  IO 异常
	 * @throws IoFilterException IoFilter 异常
	 */
	public static void onConnect(Event event) throws SendMessageException, IOException, IoFilterException  {

		IoSession session = event.getSession();
		
		// SSL 握手
		if (session!=null && session.getSSLParser() != null && !session.getSSLParser().isHandShakeDone()) {
			try {
				session.getSSLParser().doHandShake();
			} catch (IOException e) {
				Logger.error("SSL hand shake failed: "+e.getMessage(),e);
			}
		}

		SocketContext socketContext = event.getSession().socketContext();
		if (socketContext != null && session != null) {
			Object result = socketContext.handler().onConnect(session);
			result =filterEncoder(session,result);
			sendMessage(session, result);
		}
	}

	/**
	 * 连接断开事件 断开后出发
	 * 
	 * @param event
	 *            事件对象
	 */
	public static void onDisconnect(Event event) {
		SocketContext socketContext = event.getSession().socketContext();
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
	 * @throws SendMessageException  消息发送异常
	 * @throws IOException  IO 异常
	 * @throws IoFilterException IoFilter 异常
	 */
	public static void onRead(Event event) throws IOException, SendMessageException, IoFilterException {
		SocketContext socketContext = event.getSession().socketContext();
		IoSession session = event.getSession();
		if (socketContext != null && session != null) {
			ByteBuffer byteBuffer = null;


			MessageLoader messageLoader = session.getMessageLoader();

			//如果没有使用分割器,则跳过
			if(!messageLoader.isUseSpliter()){
				return;
			}

			// 循环读取完整的消息包.
			// 由于之前有消息分割器在工作,所以这里读取的消息都是完成的消息包.
			// 有可能缓冲区没有读完
			// 按消息包出发 onRecive 事件
			while (session.getByteBufferChannel().size() > 0) {

				byteBuffer = messageLoader.read();

				// 如果读出的消息为 null 则关闭连接
				if (byteBuffer == null) {
					session.close();
					return;
				}

				Object result = byteBuffer;

				// -----------------Filter 解密处理-----------------
				result = filterDecoder(session,result);
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
					result = filterEncoder(session,result);
					// ---------------------------------------------------

					// 发送消息
					sendMessage(session, result);
				}
			}

			TByteBuffer.release(byteBuffer);
		}
	}

	/**
	 * 使用过滤器过滤解码结果
	 * @param session      Session 对象
	 * @param result	   需解码的对象
	 * @return  解码后的对象
	 * @throws IoFilterException 过滤器异常
	 */
	public static Object filterDecoder(IoSession session,Object result) throws IoFilterException{
		Chain<IoFilter> filterChain = session.socketContext().filterChain().clone();
		while (filterChain.hasNext()) {
			IoFilter fitler = filterChain.next();
			result = fitler.decode(session, result);
		}
		filterChain.clear();
		return result;
	}
	
	/**
	 * 使用过滤器编码结果
	 * @param session      Session 对象
	 * @param result	   需编码的对象
	 * @return  编码后的对象
	 * @throws IoFilterException 过滤器异常
	 */
	public static Object filterEncoder(IoSession session,Object result) throws IoFilterException{
		Chain<IoFilter> filterChain = session.socketContext().filterChain().clone();
		filterChain.rewind();
		while (filterChain.hasPrevious()) {
			IoFilter fitler = filterChain.previous();
			result = fitler.encode(session, result);
		}
		filterChain.clear();
		return result;
	}
	
	
	/**
	 * 发送完成事件 发送后出发
	 * 
	 * @param event
	 *            事件对象
	 * @param obj
	 *            发送的对象
	 * @throws IOException IO 异常
	 */
	public static void onSent(Event event, Object obj) throws IOException {
		SocketContext socketContext = event.getSession().socketContext();
		if (socketContext != null) {
			IoSession session = event.getSession();
			socketContext.handler().onSent(session, obj);

			//如果 obj 是
			if(obj instanceof ByteBuffer){
				TByteBuffer.release(TObject.cast(obj));
			}
		}
	}

	/**
	 * 异常产生事件 异常产生侯触发
	 * 
	 * @param event
	 *            事件对象
	 * @param e 异常对象
	 */
	public static void onException(Event event, Exception e) {
		if (event != null 
				&& event.getSession() != null 
				&& event.getSession().socketContext() != null) {
			SocketContext socketContext = event.getSession().socketContext();
			IoSession session = event.getSession();

			if (socketContext.handler() != null) {
				socketContext.handler().onException(session, e);
			} 
		}
	}

	/**
	 * 消息发送
	 * 
	 * @param session Session 对象
	 * @param sendObj 发送的对象
	 * @throws SendMessageException  消息发送异常
	 */
	public static void sendMessage(IoSession session, Object sendObj) throws SendMessageException {
		try {
			ByteBuffer resultBuf = null;
			// 根据消息类型,封装消息
			if (sendObj != null) {
				if (sendObj instanceof ByteBuffer) {
					resultBuf = TObject.cast(sendObj);
					resultBuf.rewind();
				} else if (sendObj instanceof String) {
					String sendString = TObject.cast(sendObj);
					resultBuf = ByteBuffer.wrap(sendString.getBytes());
				} else if (sendObj instanceof byte[]) {
					byte[] sendBuffer = TObject.cast(sendObj);
					resultBuf = ByteBuffer.wrap(sendBuffer);
				} else {
					throw new SendMessageException("Expect Object type is 'java.nio.ByteBuffer' or 'java.lang.String',reality got type is '"
							+ sendObj.getClass() + "'");
				}
			}

			// 发送消息
			if (resultBuf != null && session.isOpen()) {
				session.send(resultBuf);
				resultBuf.rewind();
				//Event event = new Event(session, EventName.ON_SENT, resultBuf);
				//触发发送事件
				EventTrigger.fireSentThread(session, sendObj);

				if(sendObj!=null && !sendObj.equals(resultBuf)){
					TByteBuffer.release(resultBuf);
				}
			}
		}catch(IOException e){
			throw new SendMessageException(e);
		}
	}

	/**
	 * 处理异常
	 * @param event 事件对象
     */
	public static void process(Event event) {
		if (event == null) {
			return;
		}
		EventName eventName = event.getName();
		// 根据事件名称处理事件
		try {
			if (eventName == EventName.ON_ACCEPTED) {
				SocketContext socketContext = TObject.cast(event.getSession().socketContext());
				socketContext.start();
			} else if (eventName == EventName.ON_CONNECT) {
				EventProcess.onConnect(event);
			} else if (eventName == EventName.ON_DISCONNECT) {
				EventProcess.onDisconnect(event);
			} else if (eventName == EventName.ON_RECEIVE) {
				EventProcess.onRead(event);
				event.getSession().setReceiving(false);
			} else if (eventName == EventName.ON_SENT) {
				EventProcess.onSent(event, event.getOther());
			} else if (eventName == EventName.ON_EXCEPTION) {
				EventProcess.onException(event, (Exception)event.getOther());
			}
		} catch (IOException e) {
			EventProcess.onException(event, e);
		}
	}
}
