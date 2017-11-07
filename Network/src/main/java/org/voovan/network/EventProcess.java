package org.voovan.network;

import org.voovan.Global;
import org.voovan.network.Event.EventName;
import org.voovan.network.exception.IoFilterException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.udp.UdpSocket;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.Chain;
import org.voovan.tools.TByteBuffer;
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
		socketContext.acceptStart();
	}

	/**
	 * 连接成功事件 建立连接完成后出发
	 *
	 * @param event
	 *            事件对象
	 */
	public static void onConnect(Event event) {

		IoSession session = event.getSession();

		// SSL 握手
		if (session != null && session.getSSLParser() != null && !session.getSSLParser().isHandShakeDone()) {
			try {
				if (session.getSSLParser().doHandShake() &&
						session.getByteBufferChannel().size() > 0 &&
						!session.getState().isReceive()) {

					//将握手后的剩余数据进行处理, 并触发 onRecive 事件
					ByteBufferChannel byteBufferChannel = new ByteBufferChannel();
					session.getSSLParser().unWarpByteBufferChannel(session, session.getByteBufferChannel(), byteBufferChannel);
					session.getByteBufferChannel().clear();
					session.getByteBufferChannel().writeHead(byteBufferChannel.getByteBuffer());
					byteBufferChannel.compact();
					EventTrigger.fireReceiveThread(session);
				}
			} catch (Exception e) {
				Logger.error("SSL hand shake failed", e);
				session.close();
				return;
			}
		}

		SocketContext socketContext = event.getSession().socketContext();
		if (socketContext != null && session != null) {
			Object original = socketContext.handler().onConnect(session);
			//null 不发送
			if (original != null) {
				sendMessage(session, original);
			}
		}

		//设置空闲状态
		session.getState().setConnect(false);
	}

	/**
	 * 连接断开事件 断开后出发
	 *
	 * @param event
	 *            事件对象
	 */
	public static void onDisconnect(Event event) {
		IoSession session = event.getSession();
		session.cancelIdle();
		SocketContext socketContext = event.getSession().socketContext();

		if (socketContext != null) {
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
		IoSession session = event.getSession();
		SocketContext socketContext = session.socketContext();

		if (socketContext != null) {
			ByteBuffer byteBuffer = null;

			MessageLoader messageLoader = session.getMessageLoader();

			//如果没有使用分割器,则跳过
			if(!messageLoader.isUseSpliter()){
				//设置空闲状态
				session.getState().setReceive(false);
				session.getState().receiveUnLock();
				return;
			}

			// 循环读取完整的消息包.
			// 由于之前有消息分割器在工作,所以这里读取的消息都是完成的消息包.
			// 有可能缓冲区没有读完
			// 按消息包触发 onRecive 事件
			while (session.getByteBufferChannel().size() > 0) {

				byteBuffer = messageLoader.read();

				//设置空闲状态
				session.getState().setReceive(false);
				session.getState().receiveUnLock();

				//如果读出的数据为 null 则直接返回
				if (byteBuffer == null) {
					return;
				}

				Object result = null;

				// -----------------Filter 解密处理-----------------
				result = filterDecoder(session,byteBuffer);
				// -------------------------------------------------

				// -----------------Handler 业务处理-----------------
				if (result != null) {
					IoHandler handler = socketContext.handler();
					result = handler.onReceive(session, result);
				}
				// --------------------------------------------------

				// 返回的结果不为空的时候才发送
				if (result != null) {

					//触发发送事件
					sendMessage(session, result);
				} else {
					break;
				}

			}

			TByteBuffer.release(byteBuffer);

			//如果数据没有解析完,重新触发 onRecived 事件
			if(session.getByteBufferChannel().size() > 0){
				EventTrigger.fireReceiveThread(session);
			}
		}
	}

	/**
	 * 使用过滤器过滤解码结果
	 * @param session      Session 对象
	 * @param readedBuffer	   需解码的对象
	 * @return  解码后的对象
	 * @throws IoFilterException 过滤器异常
	 */
	public static Object filterDecoder(IoSession session, ByteBuffer readedBuffer) throws IoFilterException{
		Object result = readedBuffer;
		Chain<IoFilter> filterChain = session.socketContext().filterChain().clone();
		while (filterChain.hasNext()) {
			IoFilter fitler = filterChain.next();
			result = fitler.decode(session, result);
			if(result == null){
				break;
			}
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
	public static ByteBuffer filterEncoder(IoSession session,Object result) throws IoFilterException{
		Chain<IoFilter> filterChain = session.socketContext().filterChain().clone();
		filterChain.rewind();
		while (filterChain.hasPrevious()) {
			IoFilter fitler = filterChain.previous();
			result = fitler.encode(session, result);
			if(result == null){
				break;
			}
		}
		filterChain.clear();

		if(result instanceof ByteBuffer) {
			return (ByteBuffer)result;
		} else if(result==null){
			return null;
		} else{
			throw new IoFilterException("Send object must be ByteBuffer, " +
					"please check you filter be sure the latest filter return Object's type is ByteBuffer.");
		}
	}

	/**
	 * 在一个独立的线程中并行的发送消息
	 *
	 * @param session Session 对象
	 * @param obj 待发送的对象
	 */
	public static void sendMessage(IoSession session, Object obj) {

		final IoSession sendSession = session;
		final Object sendObj = obj;

		//开启一个线程发送消息,不阻塞当前线程
		Global.getThreadPool().execute(new Runnable() {
			@Override
			public void run() {
				int sendCount = -1;

				try {
					// ------------------Filter 加密处理-----------------
					ByteBuffer sendBuffer = EventProcess.filterEncoder(sendSession, sendObj);
					// ---------------------------------------------------

					if (sendBuffer != null) {

						// 发送消息
						if (sendBuffer != null && sendSession.isOpen()) {
							if (sendBuffer.limit() > 0) {
								sendCount = sendSession.send(sendBuffer);
								sendBuffer.rewind();
							}
						}

						//触发发送事件
						EventTrigger.fireSent(sendSession, sendObj);
					}
				}catch(IoFilterException e){
					EventTrigger.fireException(sendSession, e);
				}

				//设置空闲状态
				sendSession.getState().setSend(false);
			}
		});
	}

	/**
	 * 发送完成事件 发送后出发
	 *
	 * @param event
	 *            事件对象
	 * @param sendObj
	 *            发送的对象
	 * @throws IOException IO 异常
	 */
	public static void onSent(Event event, Object sendObj) throws IOException {
		IoSession session = event.getSession();
		SocketContext socketContext = session.socketContext();
		if (socketContext != null) {
			socketContext.handler().onSent(session, sendObj);

			//如果 obj 是 ByteBuffer 进行释放
			if (sendObj instanceof ByteBuffer) {
				TByteBuffer.release((ByteBuffer) sendObj);
			}

			//如果是 Udp 通信则在发送完成后触发关闭事件
			if(session.socketContext() instanceof UdpSocket) {
				EventTrigger.fireDisconnectThread(session);
			}
		}
	}

	/**
	 * 空闲事件触发
	 *
	 * @param event 事件对象
	 */
	public static void onIdle(Event event) {
		SocketContext socketContext = event.getSession().socketContext();
		if (socketContext != null) {
			IoSession session = event.getSession();

			socketContext.handler().onIdle(session);
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

		IoSession session = event.getSession();

		SocketContext socketContext = event.getSession().socketContext();
		if (socketContext != null) {
			if (socketContext.handler() != null) {
				socketContext.handler().onException(session, e);
			}
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
				EventProcess.onAccepted(event);
			} else if (eventName == EventName.ON_CONNECT) {
				EventProcess.onConnect(event);
			} else if (eventName == EventName.ON_DISCONNECT) {
				//设置空闲状态
				EventProcess.onDisconnect(event);
			} else if (eventName == EventName.ON_RECEIVE) {
				EventProcess.onRead(event);
			} else if (eventName == EventName.ON_SENT) {
				EventProcess.onSent(event, event.getOther());
			} else if (eventName == EventName.ON_IDLE) {
				EventProcess.onIdle(event);
			} else if (eventName == EventName.ON_EXCEPTION) {
				EventProcess.onException(event, (Exception)event.getOther());
			}
		} catch (Exception e) {
			EventProcess.onException(event, e);
		}
	}
}
