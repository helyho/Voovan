package org.voovan.http.server;

import java.nio.ByteBuffer;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.server.websocket.WebSocketDispatcher;
import org.voovan.http.server.websocket.WebSocketFrame;
import org.voovan.http.server.websocket.WebSocketTools;
import org.voovan.http.server.websocket.WebSocketDispatcher.WebSocketEvent;
import org.voovan.http.server.websocket.WebSocketFrame.Opcode;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SocketDisconnectByRemote;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

/**
 * HttpServer Socket 事件处理类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpServerHandler implements IoHandler {
	private HttpDispatcher		httpDispatcher;
	private WebSocketDispatcher	webSocketDispatcher;
	private WebServerConfig		webConfig;
	private Timer keepAliveTimer;
	private List<IoSession> keepAliveSessionList;

	public HttpServerHandler(WebServerConfig webConfig, HttpDispatcher httpDispatcher, WebSocketDispatcher webSocketDispatcher) {
		this.httpDispatcher = httpDispatcher;
		this.webSocketDispatcher = webSocketDispatcher;
		this.webConfig = webConfig;
		keepAliveTimer = new Timer("VOOVAN@Keep_Alive_Timer");
		keepAliveSessionList = new ArrayList<IoSession>();
		initKeepAliveTimer();
	}

	
	/**
	 * 活的基于当前时间的超时毫秒值
	 * @return
	 */
	private long getTimeoutValue(){
		int keepAliveTimeout = webConfig.getKeepAliveTimeout();
		return System.currentTimeMillis()+keepAliveTimeout*1000;
	}
	
	/**
	 * 初始化连接保持 Timer
	 */
	public void initKeepAliveTimer(){

		TimerTask keepAliveTask = new TimerTask() {
			@Override
			public void run() {
				//临时保存需要清理的 session
				List<IoSession> sessionNeedRemove= new ArrayList<IoSession>();
				
				//遍历所有的 session
				for(IoSession session : keepAliveSessionList){
					long currentTimeValue = System.currentTimeMillis();
					long timeOutValue = (long) session.getAttribute("TimeOutValue");
					
					if(timeOutValue < currentTimeValue){					
						//如果超时则结束当前连接
						//触发 WebSocket close 事件
						webSocketDispatcher.fireCloseEvent(session);
						session.close();
						
						sessionNeedRemove.add(session);
					}
				}
				
				//清理已经超时的会话
				for(IoSession session : sessionNeedRemove){
					keepAliveSessionList.remove(session);
				}
				
			}
		};
		keepAliveTimer.schedule(keepAliveTask, 1 , 1000);
	}
	
	@Override
	public Object onConnect(IoSession session) {
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		// 获取默认字符集
		String defaultCharacterSet = webConfig.getCharacterSet();
	
		// Http 请求
		if (obj instanceof Request) {
			// 构造请求对象
			Request request = TObject.cast(obj);
			
			// 构造响应对象
			Response response = new Response();
			response.setCompress(webConfig.isGzip());

			// 构造 Http 请求/响应 对象
			HttpRequest httpRequest = new HttpRequest(request, defaultCharacterSet);
			HttpResponse httpResponse = new HttpResponse(response, defaultCharacterSet);

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
			return disposeWebSocket(session, TObject.cast(obj));
		}
		
		// 如果协议判断失败关闭连接
		session.close();
		return null;
	}

	/**
	 * Http 请求响应处理
	 * 
	 * @param session
	 * @param httpRequest
	 * @param httpResponse
	 * @return
	 */
	public HttpResponse disposeHttp(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		session.setAttribute("Type", "HTTP");
		
		// 处理响应请求
		httpDispatcher.processRoute(httpRequest, httpResponse);
		
		//如果是长连接则填充响应报文
		if (httpRequest.header().contain("Connection") 
				&& httpRequest.header().get("Connection").toLowerCase().contains("keep-alive")) {
			session.setAttribute("IsKeepAlive", true);
			httpResponse.header().put("Connection", httpRequest.header().get("Connection"));
		}
		
		httpResponse.header().put("Server", WebContext.getVersion());
		
		return httpResponse;
	}

	/**
	 * Http协议升级处理
	 * 
	 * @param session
	 * @param httpRequest
	 * @param httpResponse
	 * @return
	 */
	public HttpResponse disposeUpgrade(IoSession session, HttpRequest httpRequest, HttpResponse httpResponse) {
		
		//保存必要参数
		session.setAttribute("Type", "Upgrade");
		session.setAttribute("IsKeepAlive", true);
		session.setAttribute("UpgradeRequest", httpRequest);

		//初始化响应消息
		httpResponse.protocol().setStatus(101);
		httpResponse.protocol().setStatusCode("Switching Protocols");
		httpResponse.header().put("Connection", "Upgrade");
		
		if(httpRequest.header()!=null && "websocket".equalsIgnoreCase(httpRequest.header().get("Upgrade"))){
			session.setAttribute("Type", "WebSocket");
			
			httpResponse.header().put("Upgrade", "websocket");
			String webSocketKey = WebSocketTools.generateSecKey(httpRequest.header().get("Sec-WebSocket-Key"));
			httpResponse.header().put("Sec-WebSocket-Accept", webSocketKey);
			
			// WS_CONNECT WebSocket Open事件
			webSocketDispatcher.processRoute(WebSocketEvent.OPEN, httpRequest, null);
		}
		
		else if(httpRequest.header()!=null && "h2c".equalsIgnoreCase(httpRequest.header().get("Upgrade"))){
			session.setAttribute("Type", "H2C");
			
			httpResponse.header().put("Upgrade", "h2c");
			//这里写 HTTP2的实现,暂时留空
		}
		return httpResponse;
	}

	/**
	 * WebSocket 帧处理
	 * 
	 * @param session
	 * @param webSocketFrame
	 * @return
	 */
	public WebSocketFrame disposeWebSocket(IoSession session, WebSocketFrame webSocketFrame) {
		session.setAttribute("Type"			, "WebSocket");
		session.setAttribute("IsKeepAlive"	, true);
		session.setAttribute("IsClose"		, false);
		
		HttpRequest upgradeRequest = TObject.cast(session.getAttribute("UpgradeRequest"));
		
		// WS_CLOSE 如果收到关闭帧则关闭连接
		if (webSocketFrame.getOpcode() == Opcode.CLOSING) {
			// WebSocket Close事件
			webSocketDispatcher.processRoute(WebSocketEvent.CLOSE, upgradeRequest, null);
			session.setAttribute("IsClose", true);
			return WebSocketFrame.newInstance(true, Opcode.CLOSING, false, webSocketFrame.getFrameData());
		}
		// WS_PING 收到 ping 帧则返回 pong 帧
		else if (webSocketFrame.getOpcode() == Opcode.PING) {
			return WebSocketFrame.newInstance(true, Opcode.PONG, false, null);
		}
		// WS_RECIVE 文本和二进制消息出发 Recived 事件
		else if (webSocketFrame.getOpcode() == Opcode.TEXT || webSocketFrame.getOpcode() == Opcode.BINARY) {
			WebSocketFrame respWebSocketFrame = null;
			
			//判断解包是否有错
			if(webSocketFrame.getErrorCode()==0){
				respWebSocketFrame = webSocketDispatcher.processRoute(WebSocketEvent.RECIVED, upgradeRequest, webSocketFrame);
			}else{
				//解析时出现异常,返回关闭消息
				respWebSocketFrame = WebSocketFrame.newInstance(true, Opcode.CLOSING, false, ByteBuffer.wrap(WebSocketTools.intToByteArray(webSocketFrame.getErrorCode(), 2)));
			}
			return respWebSocketFrame;
		}

		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		//如果 WebSocket 关闭,则关闭对应的 Socket
		if(session.containAttribute("IsClose") && (boolean) session.getAttribute("IsClose")){
			session.close();
		}
		else if (session.containAttribute("IsKeepAlive") && (boolean) session.getAttribute("IsKeepAlive")
					&& webConfig.getKeepAliveTimeout() > 0) {
			if(!keepAliveSessionList.contains(session)){
				keepAliveSessionList.add(session);
			}
			//更新会话超时时间
			session.setAttribute("TimeOutValue", getTimeoutValue());
		}else {
			if(keepAliveSessionList.contains(session)){
				keepAliveSessionList.remove("session");
			}
			session.close();
		}
	}

	@Override
	public void onException(IoSession session, Exception e) {
		//忽略远程连接断开异常 和 超时断开异常
		if(!(e instanceof SocketDisconnectByRemote) &&
			!(e instanceof InterruptedByTimeoutException)){
			Logger.error("Http Server Error: \r\n" + e.getMessage(),e);
		}		
	}
}
