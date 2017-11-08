package org.voovan.http.websocket;

import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.tools.Chain;

import java.nio.ByteBuffer;

/**
 * WebSocket 处理句柄
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class WebSocketRouter implements Cloneable{

	protected Chain<WebSocketFilter> webSocketFilterChain;

	public WebSocketRouter(){
		webSocketFilterChain = new Chain<WebSocketFilter>();
	}

	public WebSocketRouter addFilterChain(WebSocketFilter webSocketFilter) {
		webSocketFilterChain.add(webSocketFilter);
		return this;
	}

	public WebSocketRouter clearFilterChain(WebSocketFilter webSocketFilter) {
		webSocketFilterChain.clear();
		return this;
	}

	public WebSocketRouter removeFilterChain(WebSocketFilter webSocketFilter) {
		webSocketFilterChain.remove(webSocketFilter);
		return this;
	}

	/**
	 * 过滤器解密函数,接收事件(onRecive)前调用
	 * 			onRecive事件前调用
	 * @param session  session 对象
	 * @param result   解码对象,上一个过滤器的返回值
	 * @return 解码后对象
	 * @throws WebSocketFilterException WebSocket过滤器异常
	 */
	public Object filterDecoder(WebSocketSession session, Object result) throws WebSocketFilterException {
		Chain<WebSocketFilter> tmpWebFilterChain = webSocketFilterChain.clone();
		tmpWebFilterChain.rewind();
		while (tmpWebFilterChain.hasNext()) {
			WebSocketFilter fitler = tmpWebFilterChain.next();
			result = fitler.decode(session, result);
			if(result==null){
				break;
			}
		}
		return result;
	}

	/**
	 * 使用过滤器编码结果
	 * @param session      Session 对象
	 * @param result	   需编码的对象
	 * @return  编码后的对象
	 * @throws WebSocketFilterException WebSocket过滤器异常
	 */
	public Object filterEncoder(WebSocketSession session,Object result) throws WebSocketFilterException {
		Chain<WebSocketFilter> tmpWebFilterChain = webSocketFilterChain.clone();
		tmpWebFilterChain.rewind();
		while (tmpWebFilterChain.hasPrevious()) {
			WebSocketFilter fitler = tmpWebFilterChain.previous();
			result = fitler.encode(session, result);
			if(result==null){
				break;
			}
		}

		if(result instanceof ByteBuffer || result == null) {
			return (ByteBuffer)result;
		}else{
			throw new WebSocketFilterException("Send object must be ByteBuffer, " +
					"please check you filter be sure the latest filter return Object's type is ByteBuffer.");
		}
	}

	/**
	 * websocket 连接打开
	 * @param session WebSocket 会话
	 * @return 收到的缓冲数据
	 */
	public abstract Object onOpen(WebSocketSession session);

	/**
	 * websocket 收到消息
	 * @param session WebSocket 会话
	 * @param obj 收到的缓冲数据
	 * @return 收到的缓冲数据
	 */
	public abstract Object onRecived(WebSocketSession session, Object obj);

	/**
	 * websocket 消息发送完成
	 * @param session WebSocket 会话
	 * @param obj 发送的消息
	 */
	public abstract void onSent(WebSocketSession session, Object obj);


	/**
	 * websocket 关闭
	 * @param session WebSocket 会话
	 */
	public abstract void onClose(WebSocketSession session);
}
