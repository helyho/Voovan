package org.voovan.http.server;

import org.voovan.Global;
import org.voovan.http.message.Response;
import org.voovan.network.IoSession;
import org.voovan.tools.TDateTime;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

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
	private String	characterSet;
	private IoSession socketSession;
	private static String GMT_TIME = TDateTime.formatToGMT(new Date());

	static{
		Global.getHashWheelTimer().addTask(new HashWheelTask() {
			@Override
			public void run() {
				GMT_TIME = TDateTime.formatToGMT(new Date());
			}
		}, 1);
	}

	/**
	 * 构造 HTTP 响应对象
	 * @param response     响应对象
	 * @param socketSession   Socket会话对象
	 * @param characterSet 字符集
	 */
	protected HttpResponse(Response response,String characterSet, IoSession socketSession) {
		super(response);
		this.characterSet=characterSet;
		//设置当前响应的时间
		this.header().put("Date", GMT_TIME);
		this.socketSession = socketSession;
	}

	/**
	 * 构造 HTTP 响应对象
	 * @param socketSession   Socket会话对象
	 * @param characterSet 字符集
	 */
	protected HttpResponse(String characterSet, IoSession socketSession) {
		this.characterSet=characterSet;
		//设置当前响应的时间
		this.header().put("Date", GMT_TIME);
		this.socketSession = socketSession;
	}

	public void init(String characterSet, IoSession socketSession){
		this.characterSet=characterSet;
		//设置当前响应的时间
		this.header().put("Date", GMT_TIME);
		this.socketSession = socketSession;
	}

	/**
	 * 获取 socket 会话对象
	 * @return socket 会话对象
	 */
	protected IoSession getSocketSession() {
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
		if(!super.basicSend) {
			send();
		}
		return socketSession.send(byteBuffer);
	}

	/**
	 * 重定向
	 * @param path 重定向路径
	 */
	public void redirct(String path){
		protocol().setStatus(302);
		protocol().setStatusCode("Moved Permanently");
		header().put("Location", path);
		this.body().write(" ");
	}
}
