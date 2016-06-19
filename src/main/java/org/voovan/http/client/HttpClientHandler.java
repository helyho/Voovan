package org.voovan.http.client;

import org.voovan.http.message.Response;
import org.voovan.network.IoHandler;
import org.voovan.network.IoSession;
import org.voovan.network.exception.SocketDisconnectByRemote;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

/**
 * HTTP 请求处理句柄
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpClientHandler implements IoHandler {
	private HttpClient httpClient;
	private Response response;
	private boolean haveResponse = false;
	
	public HttpClientHandler(HttpClient httpClient){
		response = null;
		this.httpClient = httpClient;
	}
	
	/**
	 * 重置状态
	 */
	public void reset(){
		haveResponse = false;
	}
	
	/**
	 * 是否有可用响应对象
	 * @return 是否有响应对象可用
	 */
	public boolean isHaveResponse() {
		return haveResponse;
	}

	/**
	 * 获取响应对象
	 * @return 获取响应对象
	 */
	public synchronized Response getResponse(){
		//等待获取 response并返回
		while(!isHaveResponse()){
			TEnv.sleep(1);
		}

		Response returnResponse = response;
		response = null;
		return returnResponse;

	}
	
	@Override
	public Object onConnect(IoSession session) {
		//变更状态
		httpClient.setStatus(HttpClientStatus.IDLE);
		return null;
	}

	@Override
	public void onDisconnect(IoSession session) {
		//变更状态
		httpClient.setStatus(HttpClientStatus.CLOSED);
	}

	@Override
	public Object onReceive(IoSession session, Object obj) {
		//确认对象是否可用
		if(obj instanceof Response){
			response = TObject.cast(obj);
			System.out.println("response"+response);
			haveResponse = true;
		}
		return null;
	}

	@Override
	public void onSent(IoSession session, Object obj) {
		//不处理这个方法
	}

	@Override
	public void onException(IoSession session, Exception e) {
		httpClient.setStatus(HttpClientStatus.CLOSED);
		if(!(e instanceof SocketDisconnectByRemote)) {
			haveResponse = true;
		}
		session.close();
		Logger.error(e);
	}

}
