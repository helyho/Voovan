package org.voovan.http.message;

import org.voovan.http.message.packet.Body;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Header;
import org.voovan.http.message.packet.ResponseProtocol;
import org.voovan.http.server.context.WebContext;
import org.voovan.network.IoSession;
import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.TByte;
import org.voovan.tools.buffer.ByteBufferChannel;
import org.voovan.tools.buffer.TByteBuffer;
import org.voovan.tools.TString;
import org.voovan.tools.exception.MemoryReleasedException;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP 响应对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Response {
	private static FastThreadLocal<StringBuilder> THREAD_STRING_BUILDER = FastThreadLocal.withInitial(()->new StringBuilder(512));

	private ResponseProtocol 	protocol;
	private Header				header;
	private List<Cookie>		cookies;
	private Body 				body;
	private boolean				isCompress;
	private boolean         	hasBody;
	protected boolean 			basicSend = false;
	private boolean 			async = false;
	private Long                mark;

	/**
	 * 构造函数
	 *
	 * @param response 响应对象
	 */
	public Response(Response response) {
		init(response);
	}

	public void init(Response response){
		this.protocol = response.protocol;
		this.header = response.header;
		this.body = response.body;
		this.cookies = response.cookies;
		this.isCompress = response.isCompress;
		this.basicSend = false;
		this.mark = response.mark;
		this.hasBody = response.hasBody;
	}

	/**
	 * 构造函数
	 */
	public Response() {
		protocol = new ResponseProtocol();
		header = new Header();
		cookies = new ArrayList<Cookie>();
		body = new Body();
		isCompress = false;
		this.basicSend = false;
	}

	public Long getMark() {
		return mark;
	}

	public void setMark(Long mark) {
		this.mark = mark;
	}

	/**
	 * 是否压缩 默认为 true
	 *
	 * @return 是否启用个压缩
	 */
	public boolean isCompress() {
		return isCompress;
	}

	/**
	 * 设置压缩属性
	 *
	 * @param isCompress 是否启用个压缩
	 */
	public void setCompress(boolean isCompress) {
		this.isCompress = isCompress;
	}

	/**
	 * 是否在路由异步响应
	 * @return true: 异步响应 false: 同步响应
	 */
	public boolean isAsync() {
		return async;
	}

	/**
	 * 是否在路由异步响应
	 * @param async true: 异步响应, false: 同步响应
	 */
	public void setAsync(boolean async) {
		this.async = async;
	}

	/**
	 * 获取协议对象
	 *
	 * @return 返回响应协议对象
	 */
	public ResponseProtocol protocol() {
		return protocol;
	}

	/**
	 * 获取 Header 对象
	 *
	 * @return HTTP-Header 对象
	 */
	public Header header() {
		return header;
	}

	/**
	 * 获取所有的Cookies对象,返回一个 List
	 *
	 * @return Cookie 对象集合
	 */
	public List<Cookie> cookies() {
		return cookies;
	}

	/**
	 * 根据 Cookie 名称取 Cookie
	 *
	 * @param name  Cookie 名称
	 * @return Cookie
	 */
	public Cookie getCookie(String name){
		for(Cookie cookie : this.cookies()){
			if(cookie !=null && name !=null && name.equals(cookie.getName())){
				return cookie;
			}
		}
		return null;
	}

	/**
	 * 获取 Body 对象
	 *
	 * @return Body 对象
	 */
	public Body body() {
		return body;
	}

	public boolean isHasBody() {
		return hasBody;
	}

	public void setHasBody(boolean hasBody) {
		this.hasBody = hasBody;
	}

	/**
	 * 根据内容构造一写必要的 Header 属性
	 */
	private void initHeader() {

		// 根据压缩属性确定 Header 的一些属性内容
		if (body.size()!=0 && isCompress) {
			header.put(HttpStatic.TRANSFER_ENCODING_STRING, HttpStatic.CHUNKED_STRING);
			header.put(HttpStatic.CONTENT_ENCODING_STRING, HttpStatic.GZIP_STRING);
		} else {
			header.put(HttpStatic.CONTENT_LENGTH_STRING, Integer.toString((int)body.size()));
		}

		if (!header.contain(HttpStatic.CONTENT_TYPE_STRING)) {
			header.put(HttpStatic.CONTENT_TYPE_STRING, HttpStatic.TEXT_HTML_STRING  + WebContext.getWebServerConfig().getResponseCharacterSet());
		} else {
			header.put(HttpStatic.CONTENT_TYPE_STRING, header.get(HttpStatic.CONTENT_TYPE_STRING) + WebContext.getWebServerConfig().getResponseCharacterSet());
		}
	}

	/**
	 * 根据 Cookie 对象,生成 HTTP 响应中的 Cookie 字符串 用于报文拼装
	 *
	 * @return Cookie 字符串
	 */
	private String genCookie() {
		StringBuilder cookieString = new StringBuilder();
		for (Cookie cookie : cookies) {
			cookieString.append("Set-Cookie: ");
			cookieString.append(cookie.toString());
			cookieString.append(HttpStatic.LINE_MARK_STRING);
		}
		return cookieString.toString();
	}

	/**
	 * 根据对象的内容,构造 Http 响应报头
	 *
	 * @return ByteBuffer 响应报文的报头
	 */
	private byte[] readHead() {

		StringBuilder stringBuilder = THREAD_STRING_BUILDER.get();
		stringBuilder.setLength(0);

		initHeader();

		stringBuilder.append(protocol.toString());

		stringBuilder.append(header.toString());

		stringBuilder.append(genCookie());

		return TString.toAsciiBytes(stringBuilder.toString());
	}


	private byte[] readEnd(){
		if (isCompress) {
			return TString.toAsciiBytes("0" + HttpStatic.BODY_MARK_STRING);
		}else{
			return TByte.EMPTY_BYTES;
		}
	}

	/**
	 * 发送数据
	 * @param session socket 会话对象
	 * @throws IOException IO异常
	 */
	public void send(IoSession session) throws IOException {

		try {
			ByteBufferChannel byteBufferChannel = session.getSendByteBufferChannel();

			//发送报文头
			ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer(); //THREAD_BYTE_BUFFER.get();

			//Socket 已断开
			if(byteBuffer==null) {
				return;
			}

			//自动扩容
			if(byteBufferChannel.available() == 0) {
				byteBufferChannel.reallocate(byteBufferChannel.capacity() + 4 * 1024);
			}

			//如果有历史数据则从历史数据尾部开始写入
			byteBuffer.position(byteBuffer.limit());
			byteBuffer.limit(byteBuffer.capacity());

			try {
				byteBuffer.put(readHead());
				byteBuffer.put(WebContext.RESPONSE_COMMON_HEADER);
			} catch (Throwable e) {
				if (!(e instanceof MemoryReleasedException)) {
					Logger.error("Response writeToChannel error: ", (Exception) e);
				}
			}

			//是否需要压缩
			if (isCompress) {
				body.compress();
			}

			//发送报文主体
			int readSize = 0;
			try {
				int totalBodySize = (int) body.size();

				while ( totalBodySize > 0) {
					//预留写入 chunked 结束符的位置
					byteBuffer.limit(byteBuffer.capacity() - 10); //预留协议字节, 换行符确认4个,长度描述符1-6个
					readSize = byteBuffer.remaining() > totalBodySize ? totalBodySize : byteBuffer.remaining();
					totalBodySize = totalBodySize - readSize;

					//判断是否需要发送 chunked 段长度
					if (isCompress() && readSize != 0) {
						String chunkedLengthLine = Integer.toHexString(readSize) + HttpStatic.LINE_MARK_STRING;
						byteBuffer.put(chunkedLengthLine.getBytes());
					}

					//重置 Bytebuffer 可用字节数为 readSize
					byteBuffer.limit(byteBuffer.position() + readSize);
					body.read(byteBuffer);

					//重置写入位置
					byteBuffer.position(byteBuffer.limit());
					byteBuffer.limit(byteBuffer.capacity());

					//判断是否需要发送 chunked 结束符号
					if (isCompress() && readSize != 0 &&  byteBuffer.remaining() > 0) {
						byteBuffer.put(HttpStatic.LINE_MARK.getBytes());
					}

					if(byteBuffer.remaining() <= 10) {
						byteBuffer.flip();
						byteBufferChannel.compact();
						session.flush();
					}
				}

				//发送报文结束符
				byteBuffer.put(readEnd());
				byteBuffer.flip();
				byteBufferChannel.compact();
			} catch (Throwable e) {
				if (!(e instanceof MemoryReleasedException)) {
					Logger.error("Response writeToChannel error: ", (Exception) e);
				}
				return;
			}

			basicSend = true;
		} finally {
			if(async) {
				session.flush();
			}
			clear();
		}
	}

	public void release(){
		body.release();
	}

	/**
	 * 从其他 Response 复制数据到当前对象
	 * 	用于非发送目的
	 * @param response 原对象
	 * @return 赋值的对象
	 */
	public Response copyFrom(Response response) {
	    return copyFrom(response, false);
	}

	/**
	 * 从其他 Response 复制数据到当前对象
	 * @param response 原对象
	 * @param useForSend 是否用户发送
	 * @return 赋值的对象
	 */
	public Response copyFrom(Response response, boolean useForSend) {

		this.protocol().setStatus(response.protocol().getStatus());
		this.protocol().setStatusCode(response.protocol().getStatusCode());
		this.header().putAll(response.header().getHeaders());
		this.body().write( response.body().getBodyBytes());
		this.cookies().addAll(response.cookies());
		this.setCompress(response.isCompress);
		this.setMark(response.getMark());
		this.setHasBody(response.hasBody);

		if(useForSend) {
			//判断是否启用压缩
			if (HttpStatic.GZIP_STRING.equals(response.header().get(HttpStatic.CONTENT_ENCODING_STRING))) {
				this.setCompress(true);
				this.header.remove(HttpStatic.CONTENT_LENGTH_STRING);
			}

			this.header.remove(HttpStatic.TRANSFER_ENCODING_STRING);
			this.header.remove(HttpStatic.CONTENT_ENCODING_STRING);
		} else {
			this.async 		= response.async;
			this.basicSend 	= response.basicSend;
		}

		return this;
	}

	/**
	 * 清理
	 */
	public void clear(){
		this.header().clear();
		this.cookies().clear();
		this.protocol().clear();
		this.body().clear();
		this.isCompress = false;
		this.basicSend = false;
		this.async = false;
		this.mark = null;
	}

	@Override
	public String toString() {
		return new String(readHead());
	}
}
