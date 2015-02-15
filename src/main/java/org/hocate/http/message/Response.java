package org.hocate.http.message;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Vector;

import org.hocate.http.message.packet.Body;
import org.hocate.http.message.packet.Cookie;
import org.hocate.http.message.packet.Header;
import org.hocate.http.message.packet.ResponseProtocol;

/**
 * HTTP 响应对象
 * 
 * @author helyho
 *
 */
public class Response {
	private ResponseProtocol	protocol;
	private Header				header;
	private List<Cookie>		cookies;
	private Body				body;
	private boolean				useCompress;

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
		this.useCompress = response.useCompress;
	}

	/**
	 * 构造函数
	 */
	public Response() {
		protocol = new ResponseProtocol();
		header = new Header();
		cookies = new Vector<Cookie>();
		body = new Body();
		useCompress = true;
	}

	/**
	 * 是否压缩 默认为 true
	 * 
	 * @return
	 */
	public boolean isUseCompress() {
		return useCompress;
	}

	/**
	 * 设置压缩属性
	 * 
	 * @param useCompress
	 */
	public void setUseCompress(boolean useCompress) {
		this.useCompress = useCompress;
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
		if (useCompress) {
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

	/**
	 * 根据对象的内容,构造 Http 响应报文
	 * 
	 * @return
	 */
	public byte[] asBytes() {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		if (protocol.getStatus() != 101) {
			initHeader();
		}

		try {
			// 处理协议行
			outputStream.write(protocol.toString().getBytes());

			// 处理 Header
			outputStream.write(header.toString().getBytes());

			// 处理 Cookie
			outputStream.write(genCookie().getBytes());

			outputStream.write("\r\n".getBytes());

			if (body.getBodyBytes().length != 0) {
				if (isUseCompress()) {

					byte[] gzipedBody = body.getGZipedBody();
					outputStream.write((Integer.toUnsignedString(gzipedBody.length, 16) + "\r\n").getBytes());
					outputStream.write(gzipedBody);
					outputStream.write("\r\n0".getBytes());

				} else {
					outputStream.write(body.getBodyBytes());
				}
				outputStream.write("\r\n".getBytes());
				outputStream.write("\r\n".getBytes());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outputStream.toByteArray();
	}

	@Override
	public String toString() {
		return new String(asBytes());
	}
}
