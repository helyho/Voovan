package org.voovan.http.client;

import org.voovan.Global;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Header;
import org.voovan.http.message.packet.Part;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioSocket;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.tools.TFile;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	private Request request;
	private Map<String, Object> parameters;
	private String charset="UTF-8";
	private String urlString;
	private boolean isWebSocket = false;

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 */
	public  HttpClient(String urlString) {
		this.urlString = urlString;
		init(urlString,5);

	}

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 * @param timeOut 超时时间
	 */
	public  HttpClient(String urlString,int timeOut) {
		this.urlString = urlString;
		init(urlString,timeOut);
	}

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 * @param charset  字符集
	 * @param timeOut  超时时间
	 */
	public  HttpClient(String urlString,String charset,int timeOut) {
		this.urlString = urlString;
		this.charset = charset;
		init(urlString,timeOut);

	}

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 * @param charset  字符集
	 */
	public  HttpClient(String urlString,String charset) {
		this.urlString = urlString;
		this.charset = charset;
		init(urlString,5);

	}

	public boolean trySSL(String urlString){
		boolean isSSL = urlString.toLowerCase().startsWith("https://");
		if(!isSSL){
			isSSL = urlString.toLowerCase().startsWith("wss://");
		}

		return isSSL;
	}

	/**
	 * 初始化函数
	 * @param urlString     主机地址
	 * @param timeOut  超时时间
     */
	private void init(String urlString,int timeOut){
		try {

			boolean isSSL = trySSL(urlString);

			String hostString = urlString;
			int port = 80;

			if(hostString.toLowerCase().startsWith("ws")){
				hostString = "http"+hostString.substring(2,hostString.length());
			}
			if(hostString.toLowerCase().startsWith("http")){
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
			request.header().put("Accept-Encoding","gzip");
			request.header().put("Connection","keep-alive");

			socket = new AioSocket(hostString, port==-1?80:port, timeOut*1000);
			socket.filterChain().add(new HttpClientFilter());
			socket.messageSplitter(new HttpMessageSplitter());

			if(isSSL){
				try {
					SSLManager sslManager = new SSLManager("TLS");
					socket.setSSLManager(sslManager);
				} catch (NoSuchAlgorithmException e) {
					Logger.error(e);
				}
			}

			if(urlString.toLowerCase().startsWith("ws")){
				Global.getThreadPool().execute(()->{
					try {
						socket.start();
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}else{
				socket.syncStart();
			}

		} catch (IOException e) {
			Logger.error("HttpClient init error. ",e);
		}
	}

	/**
	 * 开启流读取模式
	 */
	public void beginLoadStream(){
		socket.getSession().openDirectBufferRead();
	}

	/**
	 * 读取流
	 * @return 字节缓冲对象ByteBuffer
	 * @throws IOException IO异常对象
	 */
	public ByteBuffer loadStream() throws IOException {
		ByteBuffer tmpBuffer = socket.getSession().directBufferRead();
		return tmpBuffer;
	}

	/**
	 * 关闭流读取模式
	 */
	public void endLoadStream(){
		socket.getSession().closeDirectBufferRead();
	}


	/**
	 * 设置请求方法
	 * @param method  Http 请求的方法
	 * @return    HttpClient 对象
	 */
	public HttpClient setMethod(String method){
		request.protocol().setMethod(method);
		return this;
	}

	/**
	 * 设置报文形式
	 * @param bodyType  Http 报文形式
	 * @return Request.BodyType 枚举
	 */
	public HttpClient setBodyType(Request.BodyType bodyType){

		//如果之前设置过 ContentType 则不自动设置 ContentType
		if(!request.header().contain("Content-Type")) {
			if (bodyType == Request.BodyType.BODY_MULTIPART) {
				request.header().put("Content-Type", "multipart/form-data;");
			} else if (bodyType == Request.BodyType.BODY_URLENCODED) {
				request.header().put("Content-Type", "application/x-www-form-urlencoded");
			}
		}
		return this;
	}

	/**
	 * 设置请求内容
	 * @param data 请求内容
	 * @return  HttpClient 对象
	 */
	public HttpClient setData(byte[] data){
		if(data!=null) {
			request.body().write(data);
		}
		return this;
	}

	/**
	 * 设置请求内容
	 * @param data 请求内容
	 * @return  HttpClient 对象
	 */
	public HttpClient setData(String data){
		if(data!=null) {
			request.body().write(data);
		}
		return this;
	}

	/**
	 * 设置请求内容
	 * @param data 请求内容
	 * @param  charset 字符集
	 * @return  HttpClient 对象
	 */
	public HttpClient setData(String data, String charset){
		if(data!=null) {
			request.body().write(data, charset);
		}
		return this;
	}

	/**
	 * 获取请求头集合
	 * @return Header 对象
	 */
	public Header getHeader(){
		return request.header();
	}

	/**
	 * 设置请求头
	 * @param name    Header 名称
	 * @param value   Header 值
	 * @return  HttpClient 对象
	 */
	public HttpClient putHeader(String name ,String value){
		request.header().put(name, value);
		return this;
	}

	/**
	 * 获取Cookie集合
	 * @return Cookie集合
	 */
	public List<Cookie> getCookies(){
		return request.cookies();
	}

	/**
	 * 获取请求参数集合
	 * @return 参数对象
	 */
	public Map<String,Object> getParameters(){
		return parameters;
	}

	/**
	 * 设置POST多段请求
	 * 		类似 Form 的 Actiong="POST" enctype="multipart/form-data"
	 * @param part POST 请求包文对象
	 * @return  HttpClient 对象
	 */
	public HttpClient addPart(Part part){
		request.parts().add(part);
		return this;
	}

	/**
	 * 设置请求参数
	 * @param name 参数名
	 * @param value 参数值
	 * @return  HttpClient 对象
	 */
	public HttpClient putParameters(String name,Object value){
		parameters.put(name, value);
		return this;
	}

	/**
	 * 构建QueryString
	 * 	将 Map 集合转换成 QueryString 字符串
	 * 	@param parameters 用于保存拼装请求字符串参数的 Map 对象
	 *  @param charset 参数的字符集
	 * @return 请求字符串
	 */
	 public static String buildQueryString(Map<String,Object> parameters,String charset){
		String queryString = "";
		StringBuilder queryStringBuilder = new StringBuilder();
		try {
			for (Entry<String, Object> parameter : parameters.entrySet()) {
				queryStringBuilder.append(parameter.getKey());
				queryStringBuilder.append("=");
				queryStringBuilder.append(URLEncoder.encode(parameter.getValue().toString(), charset));
				queryStringBuilder.append("&");
			}
			queryString = queryStringBuilder.toString();
			queryString = queryStringBuilder.length()>0?TString.removeSuffix(queryString):queryString;
		} catch (IOException e) {
			Logger.error("HttpClient getQueryString error. ",e);
		}
		return queryString.isEmpty()? "" : queryString;
	}

	/**
	 * 构建QueryString
	 * 	将 Map 集合转换成 QueryString 字符串
	 * @return 请求字符串
	 */
	private String getQueryString(){
		return buildQueryString(parameters,charset);
	}

	/**
	 * 构建请求
	 */
	private void buildRequest(String urlString){
		request.protocol().setPath(urlString.isEmpty()?"/":urlString);
		//1.没有报文 Body,参数包含于请求URL
		if (request.getBodyType() == Request.BodyType.BODY_NOBODY) {
			String queryString = getQueryString();
			String requestPath = request.protocol().getPath();
			if(requestPath.contains("?")){
				queryString = "&"+queryString;
			}else{
				queryString = "?"+queryString;
			}
			request.protocol().setPath(request.protocol().getPath() + queryString);
		}
		//2.请求报文Body 使用Part 类型
		else if(request.getBodyType() == Request.BodyType.BODY_MULTIPART){
			try{
				for (Entry<String, Object> parameter : parameters.entrySet()) {
					Part part = new Part();
					part.header().put("name", parameter.getKey());
					if(parameter.getValue() instanceof String) {
						part.body().write(URLEncoder.encode(parameter.getValue().toString(), charset).getBytes(charset));
					}else if(parameter.getValue() instanceof File){
						part.body().write(TFile.loadFile((File) parameter.getValue()));
					}
					request.parts().add(part);
				}
			} catch (IOException e) {
				Logger.error("HttpClient buildRequest error. ",e);
			}

		}
		//3.请求报文Body 使用流类型
		else if(request.getBodyType() == Request.BodyType.BODY_URLENCODED){
			String queryString = getQueryString();
			request.body().write(TString.removePrefix(queryString),charset);
		}
	}

	/**
	 * 连接并发送请求
	 * @param urlString 请求 URL
	 * @return Response 对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public Response send(String urlString) throws SendMessageException, ReadMessageException {

		if(isWebSocket){
			throw new SendMessageException("The WebSocket is connect, you can't send http request.");
		}

		//设置默认的报文 Body 类型
		if(request.protocol().getMethod().equals("POST") && request.parts().size()>0){
			setBodyType(Request.BodyType.BODY_MULTIPART);
		}else if(request.protocol().getMethod().equals("POST") && getParameters().size()>0){
			setBodyType(Request.BodyType.BODY_URLENCODED);
		}else{
			setBodyType(Request.BodyType.BODY_NOBODY);
		}

		//构造 Request 对象
		buildRequest(TString.isNullOrEmpty(urlString)?"/":urlString);

		//发送报文
		socket.synchronouSend(request);

		Response response = (Response)socket.synchronouRead();

		//结束操作
		finished(response);

		return response;
	}

	/**
	 * 请求晚场
	 * @param response 请求对象
     */
	 private void finished(Response response){
		//传递 cookie 到 Request 对象
		if(response!=null
				&& response.cookies()!=null
				&& !response.cookies().isEmpty()){
			request.cookies().addAll(response.cookies());
		}

		//清理请求对象,以便下次请求使用
		parameters.clear();
		request.body().clear();
		request.parts().clear();
		request.header().remove("Content-Type");
		request.header().remove("Content-Length");
		request.header().remove("Content-Length");
	}

	/**
	 * 发送行数
	 * @return Response 对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public void connectWebSocket(String urlString, WebSocketRouter webSocketRouter) throws SendMessageException, ReadMessageException {
		request.header().put("Connection","Upgrade");
		request.header().put("Upgrade", "websocket");
		request.header().put("Pragma","no-cache");
		request.header().put("Origin", this.urlString);
		request.header().put("Sec-WebSocket-Version","13");
		request.header().put("Sec-WebSocket-Key","c1Mm+c0b28erlzCWWYfrIg==");
		Response response = send(urlString);
		if(response.protocol().getStatus()==101){

			isWebSocket = true;

			//这里需要效验Sec-WebSocket-Accept
			socket.getSession().setAttribute("Type","WebSocket");

			WebSocketHandler webSocketHandler = new WebSocketHandler(this, webSocketRouter);
			socket.handler(webSocketHandler);

			//触发 onOpen 方法
			webSocketRouter.setSession(socket.getSession());
			ByteBuffer buffer = webSocketRouter.onOpen();
			if(buffer!=null) {
				WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.BINARY, true, buffer);
				sendData(webSocketFrame.toByteBuffer());
				webSocketFrame.getFrameData().flip();
				webSocketRouter.onSent(webSocketFrame.getFrameData());
			}

		}else{
			//异常发送关闭帧
			WebSocketFrame errWebSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.CLOSING, false, ByteBuffer.wrap(new byte[]{}));
			sendData(errWebSocketFrame.toByteBuffer());
			throw new ReadMessageException("Connect WebSocket error!");
		}
	}

	private void sendData(ByteBuffer buffer){
		try {
			socket.getSession().send(buffer);
		} catch (IOException e) {
			Logger.error("WebSocket on connect send data error!", e);
		}
	}

	/**
	 * 发送行数
	 * @return Response 对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public Response send() throws SendMessageException, ReadMessageException {
		return send("/");
	}


	/**
	 * 关闭 HTTP 连接
	 */
	public void close(){
		socket.close();
	}

	/**
	 * 判断是否处于连接状态
	 * @return 是否连接
	 */
	public boolean isConnect(){
		return socket.isConnected();
	}

}
