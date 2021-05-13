package org.voovan.http.client;

import org.voovan.Global;
import org.voovan.http.HttpRequestType;
import org.voovan.http.message.HttpStatic;
import org.voovan.http.message.Request;
import org.voovan.http.message.Response;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Header;
import org.voovan.http.message.packet.Part;
import org.voovan.http.server.HttpRequest;
import org.voovan.http.server.HttpSessionState;
import org.voovan.http.server.WebServerHandler;
import org.voovan.http.server.WebSocketDispatcher;
import org.voovan.http.websocket.WebSocketFrame;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.http.websocket.WebSocketSession;
import org.voovan.http.websocket.WebSocketType;
import org.voovan.http.websocket.exception.WebSocketFilterException;
import org.voovan.network.IoSession;
import org.voovan.network.SSLManager;
import org.voovan.network.exception.ReadMessageException;
import org.voovan.network.exception.SendMessageException;
import org.voovan.network.handler.SynchronousHandler;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.network.tcp.TcpSocket;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.pool.PooledObject;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

/**
 * HTTP 请求调用
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpClient extends PooledObject implements Closeable{

	public final static String DEFAULT_USER_AGENT = "Voovan Http Client " + Global.getVersion();

	private TcpSocket socket;
	private HttpRequest httpRequest;
	private Map<String, Object> parameters;
	private String charset="UTF-8";
	private String urlString;
	private String initLocation;
	private boolean isSSL = false;
	private boolean isWebSocket = false;
	private WebSocketRouter webSocketRouter;
	private String hostString;
	private SynchronousHandler synchronousHandler;
	private AsyncHandler asyncHandler;
	private boolean paramInUrl = false;

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 */
	public  HttpClient(String urlString) {
		this.urlString = urlString;
		init(urlString, 5);

	}

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 * @param timeout 超时时间
	 */
	public  HttpClient(String urlString,int timeout) {
		this.urlString = urlString;
		init(urlString, timeout);
	}

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 * @param charset  字符集
	 * @param timeout  超时时间
	 */
	public  HttpClient(String urlString,String charset,int timeout) {
		this.urlString = urlString;
		this.charset = charset;
		init(urlString, timeout);

	}

	/**
	 * 构建函数
	 * @param urlString 请求的 URL 地址
	 * @param charset  字符集
	 */
	public  HttpClient(String urlString,String charset) {
		this.urlString = urlString;
		this.charset = charset;
		init(urlString, 5);

	}

	private boolean trySSL(String urlString){
		boolean isSSL = urlString.toLowerCase().startsWith("https://");
		if(!isSSL){
			isSSL = urlString.toLowerCase().startsWith("wss://");
		}

		return isSSL;
	}

	/**
	 * 初始化函数
	 * @param urlString     主机地址
	 * @param timeout  超时时间
	 */
	private void init(String urlString, int timeout){
		try {

			isSSL = trySSL(urlString);

			hostString = urlString;
			int port = 80;

			if(hostString.toLowerCase().startsWith("ws")){
				hostString = "http"+hostString.substring(2,hostString.length());
			}

			if(hostString.toLowerCase().startsWith("http")){
				URL url = new URL(hostString);
				hostString = url.getHost();
				port = url.getPort();
			}

			int parhStart = urlString.indexOf("/", 8);
			if(parhStart > 8) {
				this.initLocation = urlString.substring(parhStart);
			}

			if(port==-1 && !isSSL){
				port = 80;
			}else if(port==-1 && isSSL){
				port = 443;
			}

			parameters = new LinkedHashMap<String, Object>();

			socket = new TcpSocket(hostString, port==-1?80:port, timeout*1000);
			socket.filterChain().add(new HttpClientFilter(this));
			socket.messageSplitter(new HttpMessageSplitter());

			if(isSSL){
				try {
					SSLManager sslManager = new SSLManager("TLS");
					socket.setSSLManager(sslManager);
				} catch (NoSuchAlgorithmException e) {
					Logger.error(e);
					throw new HttpClientException("HttpClient init SSL failed:", e);
				}
			}

			socket.syncStart();

			httpRequest = new HttpRequest(this.charset, socket.getSession());
			initHeader();

			asyncHandler = new AsyncHandler(this);

			synchronousHandler = (SynchronousHandler) socket.handler();

			//初始化 Session.attachment
			//[0] HttpSessionState
			//[1] SocketFilterChain
			//[2] WebSocketFilter
			//[3] Response
			Object[] attachment = new Object[4];
			attachment[0] = new HttpSessionState();
			attachment[3] = new Response();
			socket.getSession().setAttachment(attachment);

		} catch (IOException e) {
			Logger.error("HttpClient init failed",e);
			throw new HttpClientException("HttpClient init failed, on connect to " + urlString, e);
		}
	}

	/**
	 * 获取参数是否封装在 URL 中, 仅在 POST 等请求中有效
	 * @return true:是, false: 否
	 */
	public boolean isParamInUrl() {
		return paramInUrl;
	}

	/**
	 * 设置是否将参数封装在 URL 中, 仅在 POST 等请求中有效
	 * @param paramInUrl true:是, false: 否
	 * @return  HttpClient 对象
	 */
	public HttpClient setParamInUrl(boolean paramInUrl) {
		this.paramInUrl = paramInUrl;
		return this;
	}

	/**
	 * 重新初始化 http 头
	 * @return  HttpClient 对象
	 */
	public HttpClient initHeader(){
		//初始化请求参数,默认值
		httpRequest.header().put("Host", hostString);
		httpRequest.header().put("Pragma", "no-collection");
		httpRequest.header().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		httpRequest.header().put("User-Agent", DEFAULT_USER_AGENT);
		httpRequest.header().put("Accept-Encoding","gzip");
		httpRequest.header().put("Connection","keep-alive");
		return this;
	}

	/**
	 * 获取 Socket 连接
	 * @return Socket对象
	 */
	protected TcpSocket getSocket(){
		return socket;
	}

	public HttpRequest getHttpRequest() {
		return httpRequest;
	}

	/**
	 * 直接发送流
	 * @param buffer 字节缓冲对象ByteBuffer
	 */
	public void sendStream(ByteBuffer buffer) {
		socket.getSession().send(buffer);
	}

	/**
	 * 读取流
	 * @return 字节缓冲对象ByteBuffer
	 * @throws IOException IO异常对象
	 */
	public ByteBuffer loadStream() throws IOException {
		IoSession session = socket.getSession();

		ByteBuffer tmpBuffer = ByteBuffer.allocate(socket.getReadBufferSize());
		session.socketSelector().select();

		int readSize = session.read(tmpBuffer);

		if(session.getAttribute("SocketException") instanceof Exception){
			session.close();
			return null;
		}else if(readSize > 0) {
			return tmpBuffer;
		} else if(readSize == 0){
			tmpBuffer.flip();
		}else if(readSize == -1){
			return null;
		}

		return tmpBuffer;
	}

	/**
	 * 设置请求方法
	 * @param method  Http 请求的方法
	 * @return    HttpClient 对象
	 */
	public HttpClient setMethod(String method){
		httpRequest.protocol().setMethod(method.toUpperCase());
		return this;
	}

	/**
	 * 设置报文形式
	 * @param bodyType  Http 报文形式
	 * @return Request.BodyType 枚举
	 */
	public HttpClient setBodyType(Request.RequestType bodyType){

		//如果之前设置过 ContentType 则不自动设置 ContentType
		if(!httpRequest.header().contain("Content-Type")) {
			if (bodyType == Request.RequestType.BODY_MULTIPART) {
				httpRequest.header().put("Content-Type", "multipart/form-data;");
			} else if (bodyType == Request.RequestType.BODY_URLENCODED) {
				httpRequest.header().put("Content-Type", "application/x-www-form-urlencoded");
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
			httpRequest.body().write(data);
		}
		return this;
	}

	/**
	 * 设置请求内容
	 * @param data 请求内容
	 * @return  HttpClient 对象
	 */
	public HttpClient setData(String data){
		return setData(null, data, charset);
	}

	/**
	 * 设置请求内容
	 * @param contentType  ContentType 类型
	 * @param data 请求内容
	 * @return  HttpClient 对象
	 */
	public HttpClient setData(String contentType, String data){
		return setData(null, data, charset);
	}

	/**
	 * 设置请求内容
     * @param contentType  ContentType 类型
	 * @param data 请求内容
	 * @param  charset 字符集
	 * @return  HttpClient 对象
	 */
	public HttpClient setData(String contentType, String data, String charset){
		if(data!=null) {
			if(contentType!=null) {
				httpRequest.header().put("Content-Type", contentType);
			}
			httpRequest.body().write(data, charset);
		}
		return this;
	}

	/**
	 * 设置请求内容
	 * @param data 请求内容
	 * @param  charset 字符集
	 * @return  HttpClient 对象
	 */
	public HttpClient setJsonData(Object data, String charset){
		return setData("application/json", JSON.toJSON(data), charset);
	}

	/**
	 * 设置请求内容
	 * @param data 请求内容
	 * @return  HttpClient 对象
	 */
	public HttpClient setJsonData(Object data){
		return setData("application/json", JSON.toJSON(data), charset);
	}


	/**
	 * 获取请求头集合
	 * @return Header 对象
	 */
	public Header getHeader(){
		return httpRequest.header();
	}

	/**
	 * 设置请求头
	 * @param name    Header 名称
	 * @param value   Header 值
	 * @return  HttpClient 对象
	 */
	public HttpClient putHeader(String name ,String value){
		httpRequest.header().put(name, value);
		return this;
	}

	/**
	 * 获取Cookie集合
	 * @return Cookie集合
	 */
	public List<Cookie> getCookies(){
		return httpRequest.cookies();
	}

	/**
	 * 获取请求参数集合
	 * @return 参数对象
	 */
	public Map<String,Object> getParameters(){
		return parameters;
	}

	/**
	 * 设置请求参数
	 * @param name 参数名
	 * @param value 参数值
	 * @return  HttpClient 对象
	 */
	public HttpClient putParameters(String name, String value){
		parameters.put(name, value);
		return this;
	}

	/**
	 * 设置POST多段请求
	 * 		类似 Form 的 Actiong="POST" enctype="multipart/form-data"
	 * @param part POST 请求包文对象
	 * @return  HttpClient 对象
	 */
	public HttpClient addPart(Part part){
		httpRequest.parts().add(part);
		return this;
	}

	/**
	 * 上传文件
	 * @param name 参数名
	 * @param file 文件对象
	 */
	public void uploadFile(String name, File file){
		setBodyType(Request.RequestType.BODY_MULTIPART);
		parameters.put(name, file);
	}

	/**
	 * 构建QueryString
	 * 	将 Map 集合转换成 QueryString 字符串
	 * 	@param parameters 用于保存拼装请求字符串参数的 Map 对象
	 *  @param charset 参数的字符集
	 * @return 请求字符串
	 */
	public static String buildQueryString(Map<String,Object> parameters, String charset){
		String queryString = "";
		StringBuilder queryStringBuilder = new StringBuilder();
		try {
			for (Entry<String, Object> parameter : parameters.entrySet()) {
				queryStringBuilder.append(parameter.getKey());
				queryStringBuilder.append("=");
				queryStringBuilder.append(URLEncoder.encode(TObject.nullDefault(parameter.getValue(),"").toString(), charset));
				queryStringBuilder.append("&");
			}
			queryString = queryStringBuilder.toString();
			queryString = queryStringBuilder.length()>0? TString.removeSuffix(queryString):queryString;
		} catch (IOException e) {
			Logger.error("HttpClient getQueryString error",e);
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
		httpRequest.protocol().setPath(urlString.isEmpty()?"/":urlString);
		//1.没有报文 Body,参数包含于请求URL
		if (httpRequest.getBodyType() == Request.RequestType.NORMAL || paramInUrl) {
			String queryString = getQueryString();
			if(!TString.isNullOrEmpty(queryString)) {
				String requestPath = httpRequest.protocol().getPath();
				if (requestPath.contains("?")) {
					queryString = "&" + queryString;
				} else {
					queryString = "?" + queryString;
				}
				httpRequest.protocol().setPath(httpRequest.protocol().getPath() + queryString);
			}
		}
		//2.请求报文Body 使用Part 类型
		else if(httpRequest.getBodyType() == Request.RequestType.BODY_MULTIPART){
			try{
				for (Entry<String, Object> parameter : parameters.entrySet()) {
					Part part = new Part();
					part.header().put("name", parameter.getKey());
					if(parameter.getValue() instanceof String) {
						part.body().changeToBytes();
						part.body().write(URLEncoder.encode(parameter.getValue().toString(), charset).getBytes(charset));
					}else if(parameter.getValue() instanceof File){
						File file = (File) parameter.getValue();
						//参数类型如果是文件则默认采用文件的形式
						part.body().changeToFile(file);
						part.header().put("filename", file.getName());
					}
					httpRequest.parts().add(part);
				}
			} catch (IOException e) {
				Logger.error("HttpClient buildRequest error",e);
			}

		}
		//3.请求报文Body 使用流类型
		else if(httpRequest.getBodyType() == Request.RequestType.BODY_URLENCODED){
			String queryString = getQueryString();
			httpRequest.body().write(queryString, charset);
		}

		//设置默认的报文 Body 类型
		if (httpRequest.protocol().getMethod().equals(HttpStatic.POST_STRING) && httpRequest.parts().size() > 0) {
			setBodyType(Request.RequestType.BODY_MULTIPART);
		} else if (httpRequest.protocol().getMethod().equals(HttpStatic.POST_STRING) && parameters.size() > 0) {
			setBodyType(Request.RequestType.BODY_URLENCODED);
		} else {
			setBodyType(Request.RequestType.NORMAL);
		}
	}
	/**
	 * 连接并发送请求
	 * @param location 请求 URL
	 * @return Response 对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public Response send(String location) throws SendMessageException, ReadMessageException {
	    return commonSend(location, null);
	}

	/**
	 * 连接并发送请求,异步获得响应
	 * @param location 请求 URL
	 * @param async 异步响应消费对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public void asyncSend(String location,  Consumer<Response> async) throws SendMessageException, ReadMessageException {
		commonSend(location, async);
	}

	/**
	 * 连接并发送请求
	 * @param location 请求 URL
	 * @param async 异步响应消费对象
	 * @return Response 对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	private synchronized Response commonSend(String location, Consumer<Response> async) throws SendMessageException, ReadMessageException {

		IoSession session = socket.getSession();

		if (isWebSocket) {
			throw new SendMessageException("The WebSocket is connect, you can't send an http request.");
		}

		if(!TEnv.wait(socket.getReadTimeout(), false, ()->asyncHandler.isRunning())) {
			throw new SendMessageException("asyncHandler failed by timeout, the lastest isn't resposne, Socket will be disconnect");
		}

		//构造 Request 对象
		buildRequest(TString.isNullOrEmpty(location) ? initLocation : location);

		session.getReadByteBufferChannel().clear();
		session.getSendByteBufferChannel().clear();
		synchronousHandler.reset();

		//异步模式更新 handler
		if(async != null) {
			asyncHandler.setAsync(async);
			socket.handler(asyncHandler);
		} else {
			session.socketContext().handler(synchronousHandler);
		}

		//发送报文
		try {
			session.syncSend(httpRequest);
		} catch (Exception e) {
			throw new SendMessageException("HttpClient send to socket error", e);
		}

		Response response = null;

		//同步模式
		if (async == null) {
			try {
				Object readObject = socket.syncRead();

				//如果是异常则抛出异常
				if (readObject instanceof ReadMessageException) {
					throw (ReadMessageException)readObject;
				} else if (readObject instanceof Exception) {
					throw new ReadMessageException((Exception) readObject);
				} else {
					response = (Response) readObject;
				}

				return response;
			} catch (ReadMessageException e) {
				if (!isWebSocket) {
					throw e;
				}
			} finally {
				//结束操作
				reset(response);
			}
		}

		return null;
	}

	/**
	 * 连接并发送请求
	 * @return Response 对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public Response send() throws SendMessageException, ReadMessageException {
		return send("/");
	}

	/**
	 * 连接并发送请求,异步获得响应
	 * @param async 异步响应消费对象
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public void asyncSend(Consumer<Response> async) throws SendMessageException, ReadMessageException {
		asyncSend("/", async);
	}

	/**
	 * 发送二进制数据
	 * @param buffer 二进制数据
	 * @return 发送的字节数
	 * @throws IOException IO 异常
	 */
	public int sendBinary(ByteBuffer buffer) throws IOException {
		return this.httpRequest.send(buffer);
	}

	/**
	 * 重置
	 * 		自动保留响应 cookie 到 request 中
	 * @param response 请求对象
	 */
	protected void reset(Response response){
		//传递 cookie 到 Request 对象
		if(response!=null
				&& response.cookies()!=null
				&& !response.cookies().isEmpty()){
			httpRequest.cookies().addAll(response.cookies());
		}

		reset();
	}


	/**
	 * 重置
	 */
	public void reset(){
		httpRequest.body().changeToBytes();

		//清理请求对象,以便下次请求使用
		parameters.clear();
		List<Cookie> cookies = httpRequest.cookies();
		httpRequest.clear();  							//socketSession will be null
		httpRequest.cookies().addAll(cookies);

		//重新初始化 Header
		initHeader();

		asyncHandler.reset();
	}

	/**
	 * 升级协议
	 * @param location URL地址
	 * @return true: 升级成功, false: 升级失败
	 * @throws SendMessageException 发送消息异常
	 * @throws ReadMessageException 读取消息异常
	 */
	private void doWebSocketUpgrade(String location) throws SendMessageException, ReadMessageException {
		socket.setReadTimeout(-1);
		IoSession session = socket.getSession();

		httpRequest.header().put("Host", hostString);
		httpRequest.header().put("Connection","Upgrade");
		httpRequest.header().put("Upgrade", "websocket");
		httpRequest.header().put("Pragma","no-collection");
		httpRequest.header().put("Origin", this.urlString);
		httpRequest.header().put("Sec-WebSocket-Version","13");
		httpRequest.header().put("Sec-WebSocket-Key","c1Mm+c0b28erlzCWWYfrIg==");
		asyncSend(location, response -> {
			if(response.protocol().getStatus() == 101){
				//初始化 WebSocket
				initWebSocket();
			}
		});
	}

	/**
	 * 连接 Websocket
	 * @param location URL地址
	 * @param webSocketRouter WebSocker的路由
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public void webSocket(String location, WebSocketRouter webSocketRouter) throws SendMessageException, ReadMessageException {
		location = location == null ? initLocation : location;
		this.webSocketRouter = webSocketRouter;

		//处理升级后的消息
		doWebSocketUpgrade(location);
	}

	/**
	 * 连接 Websocket
	 * @param webSocketRouter WebSocker的路由
	 * @throws SendMessageException  发送异常
	 * @throws ReadMessageException  读取异常
	 */
	public void webSocket(WebSocketRouter webSocketRouter) throws SendMessageException, ReadMessageException {
		webSocket(null, webSocketRouter);
	}

	/**
	 * 初始化 WebSocket
	 *    在 HttpFilter 中触发
	 */
	protected void initWebSocket( ){
		//设置 WebSocket 标记
		isWebSocket = true;

		IoSession session = socket.getSession();

		WebSocketSession webSocketSession = new WebSocketSession(socket.getSession(), webSocketRouter, WebSocketType.CLIENT);
		WebSocketHandler webSocketHandler = new WebSocketHandler(this, webSocketSession, webSocketRouter);
		webSocketSession.setWebSocketRouter(webSocketRouter);

		//先注册Socket业务处理句柄,再打开消息分割器中 WebSocket 开关
		socket.handler(webSocketHandler);
		HttpSessionState httpSessionState = WebServerHandler.getSessionState(session);
		httpSessionState.setType(HttpRequestType.WEBSOCKET);

		Object result = null;

		//触发onOpen事件
		result = webSocketRouter.onOpen(webSocketSession);

		if(result!=null) {
			//封包
			ByteBuffer buffer = null;
			try {
				buffer = (ByteBuffer) WebSocketDispatcher.filterEncoder(webSocketSession, result);
				WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.TEXT, true, buffer);
				sendWebSocketData(webSocketFrame);

				Global.getHashWheelTimer().addTask(new HashWheelTask() {
					@Override
					public void run() {
						try {
							sendWebSocketData(WebSocketFrame.newInstance(true, WebSocketFrame.Opcode.PING, false, null));
						} catch (SendMessageException e) {
							e.printStackTrace();
						} finally {
							this.cancel();
						}
					}
				}, socket.getReadTimeout() / 1000, true);
			} catch (WebSocketFilterException e) {
				Logger.error(e);
			} catch (SendMessageException e) {
				Logger.error(e);
			}
		}
	}

	/**
	 * 发送 WebSocket 帧
	 * @param webSocketFrame WebSocket 帧
	 * @throws SendMessageException 发送异常
	 */
	private void sendWebSocketData(WebSocketFrame webSocketFrame) throws SendMessageException {
		socket.getSession().syncSend(webSocketFrame);
	}

	/**
	 * 关闭 HTTP 连接
	 */
	@Override
	public void close(){
		if(socket!=null) {
			Response response = ((Response) ((Object[])socket.getSession().getAttachment())[3]);
			response.release();
			socket.close();
		}
	}

	/**
	 * 判断是否处于连接状态
	 * @return 是否连接
	 */
	public boolean isConnect(){
		if(socket!=null) {
			return socket.isConnected();
		} else {
			return false;
		}
	}

	/**
	 * 静态构造方法
	 *   默认字符集 UTF-8, 默认超时时间 5s
	 * @param urlString 请求的 URL 地址
	 * @return HttpClient 对象, 返回 null 则构造的 HttpClient 建立连接失败
	 */
	public static HttpClient newInstance(String urlString) {
		HttpClient httpClient = new HttpClient(urlString);
		if(!httpClient.isConnect()) {
			return null;
		}
		return httpClient;
	}

	/**
	 * 静态构造方法
	 *   默认超时时间 5s
	 * @param urlString 请求的 URL 地址
	 * @param timeout  超时时间
	 * @return HttpClient 对象, 返回 null 则构造的 HttpClient 建立连接失败
	 */
	public static HttpClient newInstance(String urlString, int timeout) {
		HttpClient httpClient = new HttpClient(urlString, timeout);
		if(!httpClient.isConnect()) {
			return null;
		}
		return httpClient;
	}

	/**
	 * 静态构造方法
	 *   默认超时时间 5s
	 * @param urlString 请求的 URL 地址
	 * @param charset  字符集
	 * @return HttpClient 对象, 返回 null 则构造的 HttpClient 建立连接失败
	 */
	public static HttpClient newInstance(String urlString, String charset) {
		HttpClient httpClient = new HttpClient(urlString, charset);
		if(!httpClient.isConnect()) {
			return null;
		}
		return httpClient;
	}

	/**
	 * 静态构造方法
	 * @param urlString 请求的 URL 地址
	 * @param charset  字符集
	 * @param timeout  超时时间
	 * @return HttpClient 对象, 返回 null 则构造的 HttpClient 建立连接失败
	 */
	public static HttpClient newInstance(String urlString, String charset, int timeout) {
		HttpClient httpClient = new HttpClient(urlString, charset, timeout);
		if(!httpClient.isConnect()) {
			return null;
		}
		return httpClient;
	}

}
