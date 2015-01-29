package org.hocate.http.server;

import java.util.Timer;
import java.util.TimerTask;

import org.hocate.http.message.Request;
import org.hocate.http.message.Response;
import org.hocate.network.IoHandler;
import org.hocate.network.IoSession;
import org.hocate.tools.TObject;

/**
 * HttpServer 业务处理类
 * 
 * @author helyho
 *
 */
public class HttpServerHandler implements IoHandler {
	private RequestDispatch	requestDispatch;

	public HttpServerHandler(RequestDispatch requestDispatch) {
		this.requestDispatch = requestDispatch;
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
		// 构造请求
		Request request = TObject.cast(obj);
		// 构造响应报文并返回
		Response response = new Response();

		
		// 设置默认字符集
		String defaultCharacterSet = WebContext.getWebConfig("CharacterSet", "UTF-8");
		HttpRequest httpRequest = new HttpRequest(request,defaultCharacterSet);
		HttpResponse httpResponse = new HttpResponse(response,defaultCharacterSet);

		//填充远程连接的IP 地址和端口
		httpRequest.setRemoteAddres(session.remoteAddress());
		httpRequest.setRemotePort(session.remotePort());
		
		//处理响应请求
		requestDispatch.Process(httpRequest, httpResponse);
		if (request.header().contain("Connection")) {
			session.setAttribute("isKeepAlive", request.header().get("Connection"));
			response.header().put("Connection", request.header().get("Connection"));
		}
		return response;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		String isKeepAlive = session.getAttribute("isKeepAlive").toString();
		if (!isKeepAlive.equals("keep-alive")) {
			session.close();
		}else{
			keepLiveSchedule(session);
		}
	}

	@Override
	public void onException(IoSession session, Exception e) {
		e.printStackTrace();
	}
	
	/**
	 * 通过 Session 来控制 keepAlive 超时,超时后关闭连接
	 * 同一个连接第二次发送消息,超时时间重置
	 * @param session
	 */
	private void keepLiveSchedule(IoSession session){
		//取消上次的 KeepAliveTask
		if(session.getAttribute("keepAliveTimer")!=null){
			Timer  oldTimer = (Timer) session.getAttribute("keepAliveTimer");
			oldTimer.cancel();
		}
		
		//构造新的KeepAliveTask
		Timer keepAliveTimer = new Timer();
		int keepAliveTimeout = WebContext.getWebConfig("KeepAliveTimeout", 5);
		
		TimerTask keepAliveTask = new TimerTask() {
			@Override
			public void run() {
				session.close();
			}
		};
		
		keepAliveTimer.schedule(keepAliveTask, keepAliveTimeout*60*1000);
		
		session.setAttribute("keepAliveTimer", keepAliveTimer);
	}

}
