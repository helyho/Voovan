package org.hocate.network.messagePartition;

import org.hocate.network.IoSession;
import org.hocate.network.MessageParter;
import org.hocate.tools.TString;

public class HttpMessageParter implements MessageParter {

	@Override
	public boolean canPartition(IoSession session, byte[] buffer, int elapsedtime) {
		String bufferString = new String(buffer);

		// 包含\r\n\r\n,这个时候 Content-Length 可能存在
		if (bufferString.contains("\r\n\r\n")) {
			String[] contentLengthLines = TString.searchByRegex(bufferString, "Content-Length: .+[^\\r\\n]");
			// 1.包含 content Length 的则通过获取 contentLenght 来计算报文的总长度,长度相等时,返回成功
			if (contentLengthLines.length == 1) {
				int contentLength = Integer.valueOf(contentLengthLines[0].split(" ")[1]);
				int totalLength = bufferString.indexOf("\r\n\r\n") + 4 + contentLength;
				if (buffer.length == totalLength) {
					return true;
				}
			}
			// 2.不包含 ContentLength头的报文,则通过\r\n\r\n进行结尾判断,
			else if (bufferString.endsWith("\r\n\r\n")) {
				// 3.分段传输的 POST 请求报文的报文头和报文题结束标识都是\r\n\r\n,所以还要判断出现两次的\r\n\r\n 的位置不同说明报文加载完成
				if (bufferString.indexOf("\r\n\r\n") != bufferString.lastIndexOf("\r\n\r\n")) {
					// 如果是 post multipart/form-data类型,且没有指定
					// ContentLength,则需要使用--boundary--的结尾形式来判断
					String[] boundaryLines = TString.searchByRegex(bufferString, "boundary=[^ \\r\\n]+");
					if (boundaryLines.length == 1 && bufferString.trim().endsWith("--" + boundaryLines[0] + "--")) {
						return true;
					} else {
						return false;
					}
				}
				// 4.不包含 ContentLength 头的报文,且没有 body 则返回成功,例如:GET,TRACE,OPTIONS 等请求
				else {
					return true;
				} 
			}
		}

		return false;
	}

}
