package org.voovan.http.client;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.message.Request.RequestType;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Header;
import org.voovan.http.message.packet.Part;
import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioSocket;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

/**
 * HTTP 请求调用
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpClient {
	
	private AioSocket socket;
	private HttpClientHandler httpClientHandler; 
	private Request request;
	private Map<String, Object> parameters;
	private String charset="UTF-8";
	private HttpClientStatus status = HttpClientStatus.PREPARE;
	private boolean isSSL;
	
	
	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 */
	public  HttpClient(String urlString) {
		isSSL = urlString.toLowerCase().startsWith("https://");
		init(urlString,30);
		
	}
	
	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 */
	public  HttpClient(String urlString,int timeOut) {
		isSSL = urlString.toLowerCase().startsWith("https://");
		init(urlString,timeOut);
	}
	
	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 */
	public  HttpClient(String urlString,String charset,int timeOut) {
		isSSL = urlString.toLowerCase().startsWith("https://");
		this.charset = charset;
		init(urlString,timeOut);
		
	}
	
	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 */
	public  HttpClient(String urlString,String charset) {
		isSSL = urlString.toLowerCase().startsWith("https://");
		this.charset = charset;
		init(urlString,5);
		
	}
	
	private void init(String host,int timeOut){
		try {
			String hostString = host;
			int port = 80;
			if(host.toLowerCase().startsWith("http")){
				URL url = new URL(hostString);
				hostString = url.getHost();
				port = url.getPort();
			}
			
			if(port==-1 && !isSSL){
				port = 80;
			}else if(port==-1 && isSSL){
				port = 443;
			}
			
			parameters = new HashMap<String, Object>();
			
			request = new Request();
			//初始化请求参数,默认值
			request.header().put("Host", hostString);
			request.header().put("Pragma", "no-cache");
			request.header().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			request.header().put("User-Agent", "Voovan Http Client");
			request.header().put("Accept-Encoding","gzip,deflate,sdch");
			request.header().put("Connection","keep-alive");
			
			socket = new AioSocket(hostString, port==-1?80:port, timeOut*1000);
			httpClientHandler = new HttpClientHandler(this);
			socket.handler(httpClientHandler);
			socket.filterChain().add(new HttpClientFilter());
			socket.messageSplitter(new HttpMessageSplitter());
			
			if(isSSL){
				try {
					SSLManager sslManager = new SSLManager("TLS");
					socket.setSSLManager(sslManager);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
			}
			
			Thread bThread = new Thread(new BackThread());
			bThread.start();
			
			//等待连接完成后状态变更,变更为 IDLE
			while(status==HttpClientStatus.PREPARE){
				TEnv.sleep(1);
			}
		} catch (IOException e) {
			Logger.error("HttpClient init error. ",e);
		}
	}

	/**
	 * 后台线程
	 * @author helyho
	 *
	 * Voovan Framework.
	 * WebSite: https://github.com/helyho/Voovan
	 * Licence: Apache v2 License
	 */
	private class BackThread implements Runnable{
		public void run(){
			try {
				socket.start();
				Logger.info("HttpClient closed");
			} catch (IOException e) {
				Logger.error("Start HttpClient thread failed",e);
			}
		}
	}
	
	/**
	 * 设置请求方法
	 * @param method
	 */
	public HttpClient setMethod(String method){
		request.protocol().setMethod(method);
		return this;
	}
	
	/**
	 * 获取请求头集合
	 * @return
	 */
	public Header getHeader(){
		return request.header();
	}
	
	/**
	 * 设置请求头
	 * @return
	 */
	public HttpClient putHeader(String name ,String value){
		request.header().put(name, value);
		return this;
	}
	
	/**
	 * 获取Cookie集合
	 * @return
	 */
	public List<Cookie> getCookies(){
		return request.cookies();
	}
	
	/**
	 * 获取请求参数集合
	 * @return
	 */
	public Map<String,Object> getParameters(){
		return parameters;
	}
	
	/**
	 * 设置POST多段请求
	 * 		类似 Form 的 Actiong="POST" enctype="multipart/form-data"
	 * @param part
	 */
	public HttpClient addPart(Part part){
		request.parts().add(part);
		return this;
	}
	
	/**
	 * 设置请求参数
	 * @param name
	 * @param value
	 * @return
	 */
	public HttpClient putParameters(String name,Object value){
		parameters.put(name, value);
		return this;
	}
	
	public HttpClientStatus getStatus() {
		return status;
	}

	protected void setStatus(HttpClientStatus status) {
		this.status = status;
	}

	/**
	 * 构建QueryString
	 * 	将 Map 集合转换成 QueryString 字符串
	 * @return
	 */
	private String getQueryString(){
		String queryString = "";
		try {
			for (Entry<String, Object> parameter : parameters.entrySet()) {
				queryString += parameter.getKey()
						+ "="
						+ URLEncoder.encode(parameter.getValue().toString(), charset)
						+ "&";
			}
			queryString = queryString.length()>0?TString.removeSuffix(queryString):queryString;
		} catch (IOException e) {
			Logger.error("HttpClient getQueryString error. ",e);
		}
		return queryString.isEmpty()? "" :"?"+queryString;
	}

	/**
	 * 构建请求
	 */
	private void buildRequest(String urlString){

		request.protocol().setPath(urlString.isEmpty()?"/":urlString);
		String queryString = getQueryString();
		
		if (request.getType() == RequestType.GET) {
			request.protocol().setPath(request.protocol().getPath() + queryString);
		} else if(request.getType() == RequestType.POST && request.parts().size()!=0){
			try{
				for (Entry<String, Object> parameter : parameters.entrySet()) {
					Part part = new Part();
					part.header().put("name", parameter.getKey());
					part.body().write(URLEncoder.encode(parameter.getValue().toString(),charset).getBytes());
					request.parts().add(part);
				}
			} catch (IOException e) {
				Logger.error("HttpClient buildRequest error. ",e);
			}
			
		} else if(request.getType() == RequestType.POST && request.parts().isEmpty()){
			request.body().write(TString.removePrefix(queryString),charset);
		}
	}
	
	/**
	 * 连接并发送请求
	 * @return
	 * @throws IOException 
	 */
	public Response send(String urlString) throws IOException {
		if(status==HttpClientStatus.IDLE){
			
			//变更状态
			status = HttpClientStatus.WORKING;
			
			//构造 Request 对象
			buildRequest(TString.isNullOrEmpty(urlString)?"/":urlString);
			
			//发送报文
			if(isSSL){
				socket.getSession().sendSSLData(ByteBuffer.wrap(request.asBytes()));
			}else{
				socket.getSession().send(ByteBuffer.wrap(request.asBytes()));
			}
			
			//等待获取 response并返回
			while(isConnect() && !httpClientHandler.isHaveResponse()){
				TEnv.sleep(1);
			}
			
			Response response = httpClientHandler.getResponse();
			
			//结束操作
			finished(response);
			
			
			return response;
		}else{
			return null;
		}
	}
	
	 private void finished(Response response){
		//传递 cookie 到 Request 对象
		if(response!=null 
				&& response.cookies()!=null 
				&& response.cookies().size()>0){
			request.cookies().addAll(response.cookies());
		}
		
		//清理请求对象,以便下次请求使用
		parameters.clear();
		request.body().clear();
		request.parts().clear();
		request.header().remove("Content-Type");
		request.header().remove("Content-Length");
		
		//更新状态
		if(status == HttpClientStatus.WORKING){
			status = HttpClientStatus.IDLE;
		}
		
		httpClientHandler.reset();
	}
	
	public Response send() throws IOException {
		return send("/");
	}
	
	/**
	 * 关闭 HTTP 连接
	 */
	public void close(){
		status = HttpClientStatus.CLOSED;
		socket.close();
	}

	/**
	 * 判断是否处于连接状态
	 * @return
	 */
	public synchronized boolean isConnect(){
		return socket.isConnect();
	}
	
	public static void main(String[] args) throws IOException {
//		System.setProperty("javax.net.debug", "all");
		HttpClient getClient = new HttpClient("https://www.baidu.com","GB2312",60000);
		Response response  = getClient.setMethod("GET")
			.putParameters("name", "测试Get")
			.putParameters("age", "32").send("/test");
		Logger.simple(response.body().getBodyString("GB2312"));
		getClient.close();
	}
}
