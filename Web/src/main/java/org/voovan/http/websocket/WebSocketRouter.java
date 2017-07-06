package org.voovan.http.websocket;

import org.voovan.network.exception.IoFilterException;
import org.voovan.tools.Chain;

/**
 * WebSocket 处理句柄
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class WebSocketRouter implements Cloneable{
	private WebSocketSession session;

	protected Chain<WebSocketFilter> webFilterChain;

	public WebSocketRouter(){

		webFilterChain = new Chain<WebSocketFilter>();

	}

	public WebSocketRouter addFilterChain(WebSocketFilter webSocketFilter) {
		webFilterChain.add(webSocketFilter);
		return this;
	}

	public WebSocketRouter clearFilterChain(WebSocketFilter webSocketFilter) {
		webFilterChain.clear();
		return this;
	}

	public WebSocketRouter removeFilterChain(WebSocketFilter webSocketFilter) {
		webFilterChain.remove(webSocketFilter);
		return this;
	}

	/**
	 * 过滤器解密函数,接收事件(onRecive)前调用
	 * 			onRecive事件前调用
	 * @param session  session 对象
	 * @param result   解码对象,上一个过滤器的返回值
	 * @return 解码后对象
	 */
	public Object filterDecoder(WebSocketSession session, Object result) {
		Chain<WebSocketFilter> tmpWebFilterChain = webFilterChain.clone();
		tmpWebFilterChain.rewind();
		while (tmpWebFilterChain.hasNext()) {
			WebSocketFilter fitler = tmpWebFilterChain.next();
			result = fitler.decode(session, result);
		}
		return result;
	}

	/**
	 * 使用过滤器编码结果
	 * @param session      Session 对象
	 * @param result	   需编码的对象
	 * @return  编码后的对象
	 */
	public Object filterEncoder(WebSocketSession session,Object result) {
		Chain<WebSocketFilter> tmpWebFilterChain = webFilterChain.clone();
		tmpWebFilterChain.rewind();
		while (tmpWebFilterChain.hasPrevious()) {
			WebSocketFilter fitler = tmpWebFilterChain.previous();
			result = fitler.encode(session, result);
		}
		return result;
	}

	/**
	 * 设置会话
	 * @param session IoSession 会话对象
	 */
	public void setSession(WebSocketSession session){
		this.session = session;
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
