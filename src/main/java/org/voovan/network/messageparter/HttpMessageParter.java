package org.voovan.network.messageparter;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.voovan.network.IoSession;
import org.voovan.network.MessageParter;
import org.voovan.tools.TString;

public class HttpMessageParter implements MessageParter {

	private static final String	BODY_TAG	= "\r\n\r\n";

	@Override
	public boolean canPartition(IoSession session, byte[] buffer, int elapsedtime) {

		if (isHttpFrame(buffer)) {
			return true;
		} else if (isWebSocketFrame(ByteBuffer.wrap(buffer))) {
			return true;
		}

		return false;
	}

	public static boolean isHttpFrame(byte[] buffer) {
		String bufferString = new String(buffer);
		String[] boundaryLines 		= TString.searchByRegex(bufferString, "boundary=[^ \\r\\n]+");
		String[] contentLengthLines = TString.searchByRegex(bufferString, "Content-Length: .+[^\\r\\n]");

		// 包含\r\n\r\n,这个时候报文有可能加载完成
		if (bufferString.contains(BODY_TAG)) {

			// 1.包含 content Length 的则通过获取 contentLenght 来计算报文的总长度,长度相等时,返回成功
			if (contentLengthLines.length == 1) {
				int contentLength = Integer.valueOf(contentLengthLines[0].split(" ")[1]);
				int totalLength = bufferString.indexOf(BODY_TAG) + 4 + contentLength;
				if (buffer.length == totalLength) {
					return true;
				}
			}
			// 2. 如果是 HTTP 请求报文
			//    POST方法的multipart/form-data类型,且没有指定ContentLength,则需要使用--boundary--的结尾形式来判断
			else if (boundaryLines.length == 1 && bufferString.trim().endsWith("--" + boundaryLines[0] + "--")) {
				return true;
			}
			// 3. 如果是 HTTP 响应报文 chunk
			//    则trim 后判断最后一个字符是否是 0
			else if (boundaryLines.length == 0 && bufferString.trim().endsWith("0")) {
				return true;
			}
			// 4 HEAD,CONNECT,DELETE,GET,TRACE,OPTIONS等请求,没有报文内容
			else if (bufferString.startsWith("GET") || bufferString.startsWith("TRACE") || bufferString.startsWith("OPTIONS")
					|| bufferString.startsWith("HEAD") || bufferString.startsWith("DELETE") || bufferString.startsWith("CONNECT")) {
				return true;
			}

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
