package org.voovan.network.messagesplitter;

import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;

/**
 * Http 消息分割类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpMessageSplitter implements MessageSplitter {

	private static final String	BODY_TAG	= "\r\n\r\n";

	@Override
	public boolean canSplite(IoSession session, byte[] buffer) {

		if(buffer.length==0){
			return false;
		}


		if (isHttpFrame(buffer)) {
			return true;
		} else if (isWebSocketFrame(ByteBuffer.wrap(buffer))
                    && "WebSocket".equals(session.getAttribute("Type")) ) {
			return true;

		}

		return false;
	}

	public static boolean isHttpFrame(byte[] buffer) {
		try {
			String bufferString = new String(buffer,"UTF-8");

			if(bufferString.contains("\r\n")){
                String firstLine = bufferString.substring(0,bufferString.indexOf("\r\n"));
                if(!firstLine.contains("HTTP/")){
                    return false;
                }
			}

			String[] boundaryLines = TString.searchByRegex(bufferString, "boundary=[^ \\r\\n]+");
			String[] contentLengthLines = TString.searchByRegex(bufferString, "Content-Length: \\d+");
			boolean isChunked = bufferString.contains("chunked");

			// 包含\r\n\r\n,这个时候报文有可能加载完成
			if (bufferString.contains(BODY_TAG)) {
				// 1.包含 content Length 的则通过获取 contentLenght 来计算报文的总长度,长度相等时,返回成功
				if (contentLengthLines.length == 1) {
					int contentLength = Integer.parseInt(contentLengthLines[0].split(" ")[1].trim());
					int totalLength = bufferString.indexOf(BODY_TAG) + 4 + contentLength;
					if (buffer.length >= totalLength) {
						return true;
					}
				}

				// 2. 如果是 HTTP POST报文
				// POST方法的multipart/form-data类型,且没有指定ContentLength,则需要使用--boundary--的结尾形式来判断
				if (bufferString.contains("multipart/form-data")
						&& boundaryLines.length == 1
						&& bufferString.trim().endsWith("--" + boundaryLines[0].replace("boundary=", "") + "--")) {
					return true;
				}

				// 3. 如果是 HTTP 响应报文 chunk
				// 则trim 后判断最后一个字符是否是 0
				if (isChunked && bufferString.trim().endsWith("\r\n0")) {
					return true;
				}

				// 4.是否是无报文体的简单请求报文(1.Header 中没有 ContentLength / 2.非 Chunked 报文形式)
				if (contentLengthLines.length == 0 && !isChunked) {
					return true;
				}
			}
		} catch (UnsupportedEncodingException e) {
			Logger.error("This charset is unsupported.", e);
		}


		return false;
	}

	public static boolean isWebSocketFrame(ByteBuffer buffer) {
		// 接受数据的大小
		int maxpacketsize = buffer.remaining();
		// 期望数据包的实际大小
		int expectPackagesize = 2;
		if (maxpacketsize < expectPackagesize) {
			return false;
		}
		byte finByte = buffer.get();
		boolean fin = finByte >> 8 != 0;
		byte rsv = (byte) ((finByte & ~(byte) 128) >> 4);
		if (rsv != 0) {
			return false;
		}
		byte maskByte = buffer.get();
		boolean mask = (maskByte & -128) != 0;
		int payloadlength = (byte) (maskByte & ~(byte) 128);
		int optcode = (byte) (finByte & 15);

		if (!fin) {
			if (optcode == 9 || optcode == 10 || optcode == 8) {
				return false;
			}
		}

		if (payloadlength >= 0 && payloadlength <= 125) {
		} else {
			if (optcode == 9 || optcode == 10 || optcode == 8) {
				return false;
			}
			if (payloadlength == 126) {
				expectPackagesize += 2;
				byte[] sizebytes = new byte[3];
				sizebytes[1] = buffer.get();
				sizebytes[2] = buffer.get();
				payloadlength = new BigInteger(sizebytes).intValue();
			} else {
				expectPackagesize += 8;
				byte[] bytes = new byte[8];
				for (int i = 0; i < 8; i++) {
					bytes[i] = buffer.get();
				}
				long length = new BigInteger(bytes).longValue();
				if (length <= Integer.MAX_VALUE) {
					payloadlength = (int) length;
				}
			}
		}

		expectPackagesize += (mask ? 4 : 0);
		expectPackagesize += payloadlength;

		// 如果实际接受的数据小于数据包的大小则报错
		if (maxpacketsize < expectPackagesize) {
			return false;
		} else {
			return true;
		}
	}

}
