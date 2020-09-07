package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.HttpRequestType;
import org.voovan.http.message.HttpParser;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.message.Request;
import org.voovan.http.server.context.WebContext;
import org.voovan.http.server.context.WebServerConfig;
import org.voovan.http.server.exception.RouterNotFound;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketTools;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SendMessageException;
import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.TObject;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.exception.MemoryReleasedException;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.log.Logger;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

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
	private static FastThreadLocal<HttpRequest> THREAD_HTTP_REQUEST = FastThreadLocal.withInitial(()->new HttpRequest());
	private static FastThreadLocal<HttpResponse> THREAD_HTTP_RESPONSE = FastThreadLocal.withInitial(()->new HttpResponse());


	private HttpDispatcher		httpDispatcher;
	private WebSocketDispatcher	webSocketDispatcher;
	private WebServerConfig webConfig;
	private List<IoSession> keepAliveSessionList;


	public WebServerHandler(WebServerConfig webConfig, HttpDispatcher httpDispatcher, WebSocketDispatcher webSocketDispatcher) {
		this.httpDispatcher = httpDispatcher;
		this.webSocketDispatcher = webSocketDispatcher;
		this.webConfig = webConfig;
		keepAliveSessionList = new Vector<IoSession>();

		initKeepAliveTimer();
	}

	public static HttpSessionState getAttachment(IoSession session){
		Object[] attachment = (Object[]) session.getAttachment();

		HttpSessionState httpSessionState = (HttpSessionState)attachment[0];
		if(httpSessionState == null) {
			httpSessionState = new HttpSessionState();
			attachment[0] = httpSessionState;
		}

		return  httpSessionState;
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

					long timeoutValue = getAttachment(session).getKeepAliveTimeout();

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
		//初始化 Session.attachment
		//[0] HttpSessionState
		//[1] HttpFilter
		//[2] WebSocketFilter
		Object[] attachment = new Object[3];
		session.setAttachment(attachment);
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {
		HttpSessionState httpSessionState = getAttachment(session);

		if (httpSessionState.isWebSocket()) {

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

		if(WebContext.PAUSE && !request.protocol().getMethod().equals("ADMIN") &&
				!request.protocol().getMethod().equals("MONITOR")) {
			if(webConfig.getPauseURL()!=null){
				request.protocol().setPath(webConfig.getPauseURL());
			}else {
				session.close();
			}
		}
	}

	private void resetThreadLocal(){
		HttpParser.resetThreadLocal();
		THREAD_HTTP_REQUEST.set(null);
		THREAD_HTTP_RESPONSE.set(null);
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

			// 构造 Http 请求 对象
			HttpRequest httpRequest = THREAD_HTTP_REQUEST.get();
			httpRequest.init(request, defaultCharacterSet, session);
			// 构造 Http 响应 对象
			HttpResponse httpResponse = THREAD_HTTP_RESPONSE.get();
			httpResponse.init(defaultCharacterSet, session);

			HttpSessionState httpSessionState = getAttachment(session);
			httpSessionState.setHttpRequest(httpRequest);
			httpSessionState.setHttpResponse(httpResponse);

			// WebSocket协议升级处理
			if (webConfig.isEnableWebSocket() && WebSocketTools.isWebSocketUpgrade(request)) {
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
		HttpSessionState httpSessionState = getAttachment(session);

		//如果是长连接则填充响应报文
		if(httpRequest.protocol().getVersion().endsWith(HttpStatic.HTTP_11_STRING)) {
			httpSessionState.setKeepAlive(true);
		} else if (httpRequest.header().contain(HttpStatic.CONNECTION_STRING)) {
			if(httpRequest.header().get(HttpStatic.CONNECTION_STRING).toLowerCase().contains(HttpStatic.KEEP_ALIVE_STRING)) {
				httpSessionState.setKeepAlive(true);
				httpResponse.header().put(HttpStatic.CONNECTION_STRING, httpRequest.header().get(HttpStatic.CONNECTION_STRING));
			}

			if(httpRequest.header().get(HttpStatic.CONNECTION_STRING).toLowerCase().contains(HttpStatic.CLOSE_STRING)) {
				httpSessionState.setKeepAlive(false);
				httpResponse.header().remove(HttpStatic.CONNECTION_STRING);
			}
		}

        // 处理响应请求
        httpDispatcher.process(httpRequest, httpResponse);

		//Gzip 启用检测
		if(webConfig.isGzip() && httpRequest.header().contain(HttpStatic.ACCEPT_ENCODING_STRING) &&
				httpRequest.header().get(HttpStatic.ACCEPT_ENCODING_STRING).contains(HttpStatic.GZIP_STRING) &&
				httpResponse.header().get(HttpStatic.CONTENT_TYPE_STRING) != null) {
			//检查 body 大小是否启用 gzip
			if(httpResponse.body().size() > webConfig.getGzipMinSize()){
				//检查 MimeType 是否启用 gzip
				for(String gzipMimeType : webConfig.getGzipMimeType()){
					if(httpResponse.header().get(HttpStatic.CONTENT_TYPE_STRING).contains(gzipMimeType)){
						httpResponse.setCompress(true);
					}
				}
			}
		}

		return httpResponse;
	}

	/**
	 * Http协议升级处
	 *
	 * @param session    HTTP-Session 对象
	 * @param httpRequest  HTTP 请求对象
	 * @param httpResponse HTTP 响应对象
	 * @return HTTP 响应对象
	 */
	private static String upgradeStatusCode = "Switching Protocols";
	public HttpResponse disposeUpgrade(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		HttpSessionState httpSessionState = getAttachment(session);

		//如果不是匹配的路由则关闭连接
		if(webSocketDispatcher.findRouter(httpRequest)!=null){
			httpSessionState.setType(HttpRequestType.UPGRADE);

			//初始化响应消息
			httpResponse.protocol().setStatus(101);
			httpResponse.protocol().setStatusCode(upgradeStatusCode);
			httpResponse.header().put(HttpStatic.CONNECTION_STRING, HttpStatic.UPGRADE_STRING);

			//WebSocket 升级响应
			if(httpRequest.header()!=null && HttpStatic.WEB_SOCKET_STRING.equals(httpRequest.header().get(HttpStatic.UPGRADE_STRING))){
				httpResponse.header().put(HttpStatic.UPGRADE_STRING, HttpStatic.WEB_SOCKET_STRING);
				String webSocketKey = WebSocketTools.generateSecKey(httpRequest.header().get(HttpStatic.SEC_WEB_SOCKET_KEY_STRING));
				httpResponse.header().put(HttpStatic.SEC_WEB_SOCKET_ACCEPT_STRING, webSocketKey);
			}

			//http2 升级响应
			else if(httpRequest.header()!=null && "h2c".equals(httpRequest.header().get(HttpStatic.UPGRADE_STRING))){
				httpResponse.header().put(HttpStatic.UPGRADE_STRING, "h2c");
				//这里写 HTTP2的实现,暂时留空
			}
		} else {
			httpDispatcher.exceptionMessage(httpRequest, httpResponse, new RouterNotFound("Not avaliable router!"));
		}

		resetThreadLocal();
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
		HttpSessionState httpSessionState = getAttachment(session);

		ByteBufferChannel byteBufferChannel = null;
		if(!session.containAttribute("WebSocketByteBufferChannel")){
			byteBufferChannel = new ByteBufferChannel(session.socketContext().getReadBufferSize());
			session.setAttribute("WebSocketByteBufferChannel",byteBufferChannel);
		}else{
			byteBufferChannel = (ByteBufferChannel)session.getAttribute("WebSocketByteBufferChannel");
		}

		HttpRequest reqWebSocket = httpSessionState.getHttpRequest();

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
				ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();
				try {
					respWebSocketFrame = webSocketDispatcher.fireReceivedEvent(session, reqWebSocket, byteBuffer);
				} finally {
					byteBufferChannel.compact();
					byteBufferChannel.clear();
				}

			}else{
				//解析时出现异常,返回关闭消息
				respWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, ByteBuffer.wrap(WebSocketTools.intToByteArray(webSocketFrame.getErrorCode(), 2)));
			}
			return respWebSocketFrame;
		}

		return null;
	}

	private void refreshTimeout(IoSession session){
		HttpSessionState httpSessionState = getAttachment(session);

		int keepAliveTimeout = webConfig.getKeepAliveTimeout();
		long timeoutValue = System.currentTimeMillis()+keepAliveTimeout*1000;
		httpSessionState.setKeepAliveTimeout(timeoutValue);
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		HttpSessionState httpSessionState = getAttachment(session);

		HttpRequest request = httpSessionState.getHttpRequest();

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
		if(httpSessionState.isUpgrade()) {
			httpSessionState.setType(HttpRequestType.WEBSOCKET);
			httpSessionState.setKeepAlive(true);

			//触发 onOpen 事件
			WebSocketFrame webSocketFrame = webSocketDispatcher.fireOpenEvent(session, request);

			if(webSocketFrame!=null) {

				try {
					session.syncSend(webSocketFrame);
				} catch (SendMessageException e) {
					session.close();
					Logger.error("WebSocket Open event writeToChannel frame error", e);
				}
			}

			//发送第一次心跳消息
			WebSocketDispatcher.getHeartBeatWheelTimer().addTask(new HashWheelTask() {
				@Override
				public void run() {
					//发送 ping 消息
					try {
						WebSocketFrame ping = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, false, null);
						session.send(ping.toByteBuffer());
						session.flush();
					} catch (Exception e) {
						session.close();
						Logger.error("WebSocket writeToChannel Ping frame error", e);
					} finally {
						this.cancel();
					}
				}
			}, session.socketContext().getReadTimeout()/3/1000);
		}
	}

	@Override
	public void onFlush(IoSession session) {
		HttpSessionState httpSessionState = getAttachment(session);

		HttpRequest request = httpSessionState.getHttpRequest();
		//处理连接保持
		if (httpSessionState.isKeepAlive() && webConfig.getKeepAliveTimeout() > 0) {

			if (httpSessionState.isKeepLiveListContain()) {
				keepAliveSessionList.add(session);
				httpSessionState.setKeepLiveListContain(true);
			}
			//更新会话超时时间
			refreshTimeout(session);

		} else {
			keepAliveSessionList.remove(session);
			session.close();
		}
	}

	@Override
	public void onException(IoSession session, Exception e) {
		if(!(e instanceof MemoryReleasedException) && !(e instanceof TimeoutException)) {
			Logger.error("Http Server Error", e);
		}

		session.close();
	}

	@Override
	public void onIdle(IoSession session) {

	}
}
