package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketTools;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WebServer Socket 事件处理类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebServerHandler implements IoHandler {
	private HttpDispatcher		httpDispatcher;
	private WebSocketDispatcher	webSocketDispatcher;
	private WebServerConfig webConfig;
	private List<IoSession> keepAliveSessionList;


	public class SessionParam{
		public static final int TYPE = 0x1111;
		public static final int HTTP_REQUEST = 0x2222;
		public static final int HTTP_RESPONSE = 0x3333;
		public static final int KEEP_ALIVE = 0x4444;
		public static final int KEEP_ALIVE_TIMEOUT = 0x5555;
	}

	public WebServerHandler(WebServerConfig webConfig, HttpDispatcher httpDispatcher, WebSocketDispatcher webSocketDispatcher) {
		this.httpDispatcher = httpDispatcher;
		this.webSocketDispatcher = webSocketDispatcher;
		this.webConfig = webConfig;
		keepAliveSessionList = new ArrayList<IoSession>();

		initKeepAliveTimer();

	}

	/**
	 * 获取属性
	 * @param session       会话对象
	 * @param sessionParam  会话参数枚举
	 * @param <T>           范型
	 * @return  参数
	 */
	public static <T> T getAttribute(IoSession session, int sessionParam){
		return (T)session.getAttribute(sessionParam);
	}

	/**
	 *
	 * @param session       会话对象
	 * @param sessionParam  会话参数枚举
	 * @param value  参数
	 * @param <T>           范型
	 */
	public static <T> void  setAttribute(IoSession session, int sessionParam, T value){
		session.setAttribute(sessionParam, value);
	}

	/**
	 * 初始化连接保持 Timer
	 */
	public void initKeepAliveTimer(){

		Global.getHashWheelTimer().addTask(new HashWheelTask() {

			@Override
			public void run() {

				long currentTimeValue = System.currentTimeMillis();
				//遍历所有的 session
				for(int i=0; i<keepAliveSessionList.size(); i++){

					IoSession session = keepAliveSessionList.get(i);

					if(session==null){
						continue;
					}

					long timeoutValue = getAttribute(session, SessionParam.KEEP_ALIVE_TIMEOUT);

					if(timeoutValue < currentTimeValue){
						//如果超时则结束当前连接
						session.close();

						keepAliveSessionList.remove(session);
						i--;
					}
				}
			}
		} ,1);
	}

	@Override
	public Object onConnect(IoSession session) {
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {

		if ("WebSocket".equals(getAttribute(session, SessionParam.TYPE))) {

			// 触发一个 WebSocket Close 事件
			webSocketDispatcher.fireCloseEvent(session);

			//WebSocket 要考虑释放缓冲区
			ByteBufferChannel byteBufferChannel = (ByteBufferChannel)session.getAttribute("WebSocketByteBufferChannel");
			if (byteBufferChannel != null && !byteBufferChannel.isReleased()) {
				byteBufferChannel.release();
			}
		}

		//清理 IoSession
		keepAliveSessionList.remove(session);
	}

	/**
	 * Web 服务暂停检查
	 * 		如果配置文件指定了 PauseURL 则转到 PauseURL 指定的URL, 否则关闭当前连接
	 * @param session IoSession: Socket 会话对象
	 * @param request Request: HTTP 请求对象
	 */
	public void checkPause(IoSession session, Request request){

		if(WebContext.PAUSE && !request.protocol().getMethod().equals("ADMIN")){
			if(webConfig.getPauseURL()!=null){
				request.protocol().setPath(webConfig.getPauseURL());
			}else {
				session.close();
			}
		}
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		// 获取默认字符集
		String defaultCharacterSet = webConfig.getCharacterSet();

		// Http 请求
		if (obj instanceof Request) {
			// 构造请求对象
			Request request = (Request)obj;

			//检查服务是否暂停
			checkPause(session, request);

			if(!session.isConnected()){
				return null;
			}

			// 构造响应对象
			Response response = new Response();

			if(webConfig.isGzip() && request.header().contain("Accept-Encoding") &&
					request.header().get("Accept-Encoding").contains("gzip")) {
				response.setCompress(true);
			}

			// 构造 Http 请求/响应 对象
			HttpRequest httpRequest = new HttpRequest(request, defaultCharacterSet, session);
			HttpResponse httpResponse = new HttpResponse(response, defaultCharacterSet, session);

			setAttribute(session, SessionParam.HTTP_REQUEST, httpRequest);
			setAttribute(session, SessionParam.HTTP_RESPONSE, httpResponse);

			// 填充远程连接的IP 地址和端口
			httpRequest.setRemoteAddres(session.remoteAddress());
			httpRequest.setRemotePort(session.remotePort());

			// WebSocket协议升级处理
			if (WebSocketTools.isWebSocketUpgrade(request)) {
				return disposeUpgrade(session, httpRequest, httpResponse);
			}
			// Http 1.1处理
			else {
				return disposeHttp(session, httpRequest, httpResponse);
			}
		}
		//处理 WEBSocket 报文
		else if (obj instanceof WebSocketFrame) {
			return disposeWebSocket(session, (WebSocketFrame)obj);
		}

		// 如果协议判断失败关闭连接
		session.close();
		return null;
	}

	/**
	 * Http 请求响应处理
	 *
	 * @param session    HTTP-Session 对象
	 * @param httpRequest  HTTP 请求对象
	 * @param httpResponse HTTP 响应对象
	 * @return HTTP 响应对象
	 */
	public HttpResponse disposeHttp(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		setAttribute(session, SessionParam.TYPE, "HTTP");


		// 处理响应请求
		httpDispatcher.process(httpRequest, httpResponse);

		//如果是长连接则填充响应报文
		if (httpRequest.header().contain("Connection")
				&& httpRequest.header().get("Connection").toLowerCase().contains("keep-alive")) {
			setAttribute(session, SessionParam.KEEP_ALIVE, true);
			httpResponse.header().put("Connection", httpRequest.header().get("Connection"));
		}

		httpResponse.header().put("Server", WebContext.getVERSION());

		return httpResponse;
	}

	/**
	 * Http协议升级处理
	 *
	 * @param session    HTTP-Session 对象
	 * @param httpRequest  HTTP 请求对象
	 * @param httpResponse HTTP 响应对象
	 * @return HTTP 响应对象
	 */
	public HttpResponse disposeUpgrade(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {

		//如果不是匹配的路由则关闭连接
		if(webSocketDispatcher.findRouter(httpRequest)!=null){
			setAttribute(session, SessionParam.TYPE, "Upgrade");
			httpDispatcher.disposeSession(httpRequest, httpResponse);

			//初始化响应消息
			httpResponse.protocol().setStatus(101);
			httpResponse.protocol().setStatusCode("Switching Protocols");
			httpResponse.header().put("Connection", "Upgrade");

			if(httpRequest.header()!=null && "websocket".equalsIgnoreCase(httpRequest.header().get("Upgrade"))){

				httpResponse.header().put("Upgrade", "websocket");
				String webSocketKey = WebSocketTools.generateSecKey(httpRequest.header().get("Sec-WebSocket-Key"));
				httpResponse.header().put("Sec-WebSocket-Accept", webSocketKey);
			}

			else if(httpRequest.header()!=null && "h2c".equalsIgnoreCase(httpRequest.header().get("Upgrade"))){
				httpResponse.header().put("Upgrade", "h2c");
				//这里写 HTTP2的实现,暂时留空
			}
		} else {
			httpDispatcher.exceptionMessage(httpRequest, httpResponse, new RouterNotFound("Not avaliable router!"));
		}


		return httpResponse;
	}

	/**
	 * WebSocket 帧处理
	 *
	 * @param session 	HTTP-Session 对象
	 * @param webSocketFrame WebSocket 帧对象
	 * @return WebSocket 帧对象
	 */
	public WebSocketFrame disposeWebSocket(IoSession session, WebSocketFrame webSocketFrame) {

		ByteBufferChannel byteBufferChannel = null;
		if(!session.containAttribute("WebSocketByteBufferChannel")){
			byteBufferChannel = new ByteBufferChannel(session.socketContext().getBufferSize());
			session.setAttribute("WebSocketByteBufferChannel",byteBufferChannel);
		}else{
			byteBufferChannel = (ByteBufferChannel)session.getAttribute("WebSocketByteBufferChannel");
		}

		HttpRequest reqWebSocket = getAttribute(session, SessionParam.HTTP_REQUEST);

		// WS_CLOSE 如果收到关闭帧则关闭连接
		if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.CLOSING) {
			return WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, webSocketFrame.getFrameData());
		}
		// WS_PING 收到 ping 帧则返回 pong 帧
		else if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.PING) {
			return webSocketDispatcher.firePingEvent(session, reqWebSocket, webSocketFrame.getFrameData());
		}
		// WS_PING 收到 pong 帧则返回 ping 帧
		else if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.PONG) {
			refreshTimeout(session);
			webSocketDispatcher.firePoneEvent(session, reqWebSocket, webSocketFrame.getFrameData());
			return null;
		}else if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.CONTINUOUS){
			byteBufferChannel.writeEnd(webSocketFrame.getFrameData());
		}
		// WS_RECIVE 文本和二进制消息出发 Recived 事件
		else if (webSocketFrame.getOpcode() == WebSocketFrame.Opcode.TEXT || webSocketFrame.getOpcode() == WebSocketFrame.Opcode.BINARY) {

			byteBufferChannel.writeEnd(webSocketFrame.getFrameData());
			WebSocketFrame respWebSocketFrame = null;

			//判断解包是否有错
			if(webSocketFrame.getErrorCode()==0){
				respWebSocketFrame = webSocketDispatcher.fireReceivedEvent(session, reqWebSocket, byteBufferChannel.getByteBuffer());
				byteBufferChannel.compact();
				byteBufferChannel.clear();
			}else{
				//解析时出现异常,返回关闭消息
				respWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, ByteBuffer.wrap(WebSocketTools.intToByteArray(webSocketFrame.getErrorCode(), 2)));
			}
			return respWebSocketFrame;
		}

		return null;
	}

	private void refreshTimeout(IoSession session){
		int keepAliveTimeout = webConfig.getKeepAliveTimeout();
		long timeoutValue = System.currentTimeMillis()+keepAliveTimeout*1000;
		setAttribute(session, SessionParam.KEEP_ALIVE_TIMEOUT, timeoutValue);

	}

	@Override
	public void onSent(IoSession session, Object obj) {
		HttpRequest request = getAttribute(session,SessionParam.HTTP_REQUEST);
		HttpResponse response = getAttribute(session,SessionParam.HTTP_RESPONSE);

		//WebSocket 协议处理
		if(obj instanceof WebSocketFrame){
			WebSocketFrame webSocketFrame = (WebSocketFrame)obj;

			if(webSocketFrame.getOpcode() == WebSocketFrame.Opcode.CLOSING){
				session.close();
			} else if (webSocketFrame.getOpcode() != WebSocketFrame.Opcode.PING &&
					webSocketFrame.getOpcode() != WebSocketFrame.Opcode.PONG) {
				webSocketDispatcher.fireSentEvent(session, request, webSocketFrame.getFrameData());
			}
		}

		//针对 WebSocket 的处理协议升级
		if("Upgrade".equals(getAttribute(session, SessionParam.TYPE))){
			setAttribute(session, SessionParam.TYPE, "WebSocket");
			setAttribute(session, SessionParam.KEEP_ALIVE, true);

			//触发 onOpen 事件
			WebSocketFrame webSocketFrame = webSocketDispatcher.fireOpenEvent(session, request);

			if(webSocketFrame!=null) {

				try {
					session.syncSend(webSocketFrame);
				} catch (SendMessageException e) {
					session.close();
					Logger.error("WebSocket Open event send frame error", e);
				}
			}

			//发送第一次心跳消息
			Global.getHashWheelTimer().addTask(new HashWheelTask() {
				@Override
				public void run() {
					//发送 ping 消息
					try {
						WebSocketFrame ping = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, false, null);
						session.send(ping.toByteBuffer());
					} catch (Exception e) {
						session.close();
						Logger.error("WebSocket send Ping frame error", e);
					}finally {
						this.cancel();
					}
				}
			}, session.socketContext().getReadTimeout()/3/1000);
		}

		//处理连接保持
		if ( getAttribute(session, SessionParam.KEEP_ALIVE) !=null &&
				(boolean)getAttribute(session, SessionParam.KEEP_ALIVE) &&
				webConfig.getKeepAliveTimeout() > 0) {

			if (!keepAliveSessionList.contains(session)) {
				keepAliveSessionList.add(session);
			}
			//更新会话超时时间
			refreshTimeout(session);
		} else {
			if (keepAliveSessionList.contains(session)) {
				keepAliveSessionList.remove(session);
			}
			session.close();
		}
	}

	@Override
	public void onException(IoSession session, Exception e) {
		//忽略远程连接断开异常 和 超时断开异常
		if(!(e instanceof InterruptedByTimeoutException)){
			Logger.error("Http Server Error",e);
		}
		session.close();
	}

	@Override
	public void onIdle(IoSession session) {

	}
}
