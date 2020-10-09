package org.voovan.http.server;

import org.voovan.http.message.HttpStatic;
import org.voovan.http.message.Response;
import org.voovan.network.IoSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * WebServer 响应对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpResponse extends Response {
	private volatile String	characterSet;
	private volatile IoSession socketSession;

	/**
	 * 构造 HTTP 响应对象
	 */
	public HttpResponse() {
	}


	/**
	 * 构造 HTTP 响应对象
	 * @param response     响应对象
	 * @param socketSession   Socket会话对象
	 * @param characterSet 字符集
	 */
	public HttpResponse(Response response,String characterSet, IoSession socketSession) {
		super(response);
		this.characterSet=characterSet;
		//设置当前响应的时间
		this.socketSession = socketSession;
	}

	/**
	 * 构造 HTTP 响应对象
	 * @param socketSession   Socket会话对象
	 * @param characterSet 字符集
	 */
	public HttpResponse(String characterSet, IoSession socketSession) {
		this.characterSet=characterSet;
		//设置当前响应的时间
		this.socketSession = socketSession;
	}

	public void init(String characterSet, IoSession socketSession){
		this.characterSet=characterSet;
		//设置当前响应的时间
		this.socketSession = socketSession;
	}

	/**
	 * 获取 socket 会话对象
	 * @return socket 会话对象
	 */
	public IoSession getSocketSession() {
		return socketSession;
	}

	/**
	 * 设置 socket 会话对象
	 * @param socketSession socket 会话对象
	 */
	protected void setSocketSession(IoSession socketSession) {
		this.socketSession = socketSession;
	}

	/**
	 * 获取当前默认字符集
	 *
	 * @return 默认字符集
	 */
	public String getCharacterSet() {
		return characterSet;
	}

	/**
	 * 设置当前默认字符集
	 *
	 * @param characterSet 默认字符集
	 */
	public void setCharacterSet(String characterSet) {
		this.characterSet = characterSet;
	}

	public HttpResponse getAsyncResponse() {
		this.setAsync(true);
		HttpResponse httpResponse = new HttpResponse();
		httpResponse.copyFrom(this);

		return httpResponse;
	}

	/**
	 * 写入一个 byte 数组
	 *
	 * @param bytes  byte 数组
	 */
	public void write(byte[] bytes) {
		body().write(bytes);
	}

	/**
	 * 写入一个 byte 数组
	 *
	 * @param bytes  byte 数组
	 * @param offset 偏移量
	 * @param length 写入长度
	 */
	public void write(byte[] bytes, int offset, int length) {
		body().write(bytes, offset, length);
	}

	/**
	 * 写入一个字符串
	 *
	 * @param strs 字符串
	 */
	public void write(String strs) {
		if(strs!=null){
			body().write(strs, characterSet);
		}
	}


	/**
	 * 写入 body 对象, 自动转换为 json,默认 UTF-8
	 * @param obj body 对象
	 */
	public void writeObject(Object obj){
		body().writeObject(obj);
	}

	/**
	 * 写入 body 字符串 自动转换为 json
	 * @param obj body 对象
	 * @param charset 字符集
	 */
	public void writeObject(Object obj, String charset){
		body().writeObject(obj, charset);
	}

	/**
	 * 发送响应
	 * @throws IOException IO 异常
	 */
	public void send() throws IOException {
		super.send(socketSession);
	}

	/**
	 * 追加形式发送数据
	 * @param byteBuffer 发送的缓冲区
	 * @return 发送的字节数
	 * @throws IOException IOException IO 异常
	 */
	public int send(ByteBuffer byteBuffer) throws IOException {
		if(!super.isSend) {
			send();
		}
		return socketSession.send(byteBuffer);
	}

	/**
	 * 将数据发送到 Socket 缓存
	 */
	public void flush(){
		socketSession.flush();
	}

	/**
	 * 重定向
	 * @param path 重定向路径
	 */
	public void redirct(String path){
		protocol().setStatus(302);
		protocol().setStatusCode("Moved Permanently");
		header().put(HttpStatic.LOCATION_STRING, path);
		this.body().write(" ");
	}

	public HttpResponse copyFrom(HttpResponse response, boolean useForSend) {
		super.copyFrom(response, useForSend);
		this.setCharacterSet(response.getCharacterSet());

		if(!useForSend) {
			this.setSocketSession(response.getSocketSession());
		}

		return this;
	}

	public HttpResponse copyFrom(HttpResponse response) {
		this.copyFrom(response, false);
		return this;
	}
}
