package org.voovan.http.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.voovan.http.message.packet.Body;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Header;
import org.voovan.http.message.packet.ResponseProtocol;
import org.voovan.tools.log.Logger;

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
	private ResponseProtocol	protocol;
	private Header				header;
	private List<Cookie>		cookies;
	private Body				body;
	private boolean				isCompress;

	/**
	 * 构造函数
	 * 
	 * @param response
	 */
	protected Response(Response response) {
		this.protocol = response.protocol;
		this.header = response.header;
		this.body = response.body;
		this.cookies = response.cookies;
		this.isCompress = response.isCompress;
	}

	/**
	 * 构造函数
	 */
	public Response() {
		protocol = new ResponseProtocol();
		header = new Header();
		cookies = new ArrayList<Cookie>();
		body = new Body();
		isCompress = true;
	}

	/**
	 * 是否压缩 默认为 true
	 * 
	 * @return
	 */
	public boolean isCompress() {
		return isCompress;
	}

	/**
	 * 设置压缩属性
	 * 
	 * @param useCompress
	 */
	public void setCompress(boolean isCompress) {
		this.isCompress = isCompress;
	}

	/**
	 * 获取协议对象
	 * 
	 * @return
	 */
	public ResponseProtocol protocol() {
		return protocol;
	}

	/**
	 * 获取 Header 对象
	 * 
	 * @return
	 */
	public Header header() {
		return header;
	}

	/**
	 * 获取所有的Cookies对象,返回一个 List
	 * 
	 * @return
	 */
	public List<Cookie> cookies() {
		return cookies;
	}

	/**
	 * 获取 Body 对象
	 * 
	 * @return
	 */
	public Body body() {
		return body;
	}

	/**
	 * 根据内容构造一写必要的 Header 属性
	 */
	private void initHeader() {
		// 根据压缩属性确定 Header 的一些属性内容
		if (isCompress) {
			header.put("Transfer-Encoding", "chunked");
			header.put("Content-Encoding", "gzip");
		} else {
			header.put("Content-Length", Integer.toString(body.getBodyBytes().length));
		}
		
		if (header.get("Content-Type") == null) {
			header.put("Content-Type", "text/html");
		}
	}

	/**
	 * 根据 Cookie 对象,生成 HTTP 响应中的 Cookie 字符串 用于报文拼装
	 * 
	 * @return
	 */
	private String genCookie() {
		String cookieString = "";
		for (Cookie cookie : cookies) {
			cookieString = cookieString + "Set-Cookie: " + cookie.toString() + "\r\n";
		}
		return cookieString;
	}

	private byte[] genBody(){
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			if (body.getBodyBytes().length != 0) {
				if (isCompress) {
					byte[] gzipedBody = body.getGZipedBody();
					// 写入 chunk 的长度
					outputStream.write((Integer.toUnsignedString(gzipedBody.length, 16) + "\r\n").getBytes());
					outputStream.write(gzipedBody);
					// chunk结束
					outputStream.write("\r\n".getBytes());
					outputStream.write("0".getBytes());

				} else {
					outputStream.write(body.getBodyBytes());
				}
				//结尾换行根据 http 协议不需要
				// 报文内容结束
//				outputStream.write("\r\n".getBytes());
				// 插入空行
//				outputStream.write("\r\n".getBytes());
				
				return outputStream.toByteArray();
			}
		} catch (IOException e) {
			Logger.error("OutputString io error.",e);
		}

		return new byte[0];
	}
	
	/**
	 * 根据对象的内容,构造 Http 响应报文
	 * 
	 * @return
	 */
	public byte[] asBytes() {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		initHeader();
		
		byte[] bodyBytes = genBody();
		
		try {
			// 处理协议行
			outputStream.write(protocol.toString().getBytes());

			// 处理 Header
			outputStream.write(header.toString().getBytes());

			// 处理 Cookie
			outputStream.write(genCookie().getBytes());

			//头结束插入空行
			outputStream.write("\r\n".getBytes());

			//插入报文内容
			outputStream.write(bodyBytes);
			
		} catch (IOException e) {
			Logger.error("OutputString io error.",e);
		}
		return outputStream.toByteArray();
	}

	@Override
	public String toString() {
		return new String(asBytes());
	}
}
