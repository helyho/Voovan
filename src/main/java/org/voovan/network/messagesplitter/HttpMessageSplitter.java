package org.voovan.network.messagesplitter;

import org.voovan.http.websocket.WebSocketTools;
import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TString;

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
	private int result = -1;


	@Override
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {

		if(byteBuffer.limit()==0){
			return -1;
		}

		result = isHttpFrame(byteBuffer);

	    if (result==-1 && "WebSocket".equals(session.getAttribute("Type")) ) {
			result = WebSocketTools.isWebSocketFrame(byteBuffer);
		}

		return result;
	}

	public static int isHttpFrame(ByteBuffer byteBuffer) {
        String bufferString = TByteBuffer.toString(byteBuffer, "UTF-8");

        if(bufferString.contains("\r\n")){
            String firstLine = bufferString.substring(0,bufferString.indexOf("\r\n"));
            if(TString.regexMatch(firstLine,"HTTP\\/\\d\\.\\d\\s\\d{3}\\s.*") <= 0){
                if(TString.regexMatch(firstLine,"^[A-Z]*\\s.*\\sHTTP\\/\\d\\.\\d") <= 0){
                    return -1;
                }
            }
        }

        String[] boundaryLines = TString.searchByRegex(bufferString, "boundary=[^ \\r\\n]+");
        String[] contentLengthLines = TString.searchByRegex(bufferString, "Content-Length: \\d+");
        boolean isChunked = bufferString.contains("chunked");

        // 包含\r\n\r\n,这个时候报文有可能加载完成
//			if (bufferString.contains(BODY_TAG)){
//				return bufferString.indexOf(BODY_TAG) + BODY_TAG.length();
//			}

        if (bufferString.contains(BODY_TAG)) {
            // 1.包含 content Length 的则通过获取 contentLenght 来计算报文的总长度,长度相等时,返回成功
            if (contentLengthLines.length == 1) {
                int contentLength = Integer.parseInt(contentLengthLines[0].split(" ")[1].trim());
                int totalLength = bufferString.indexOf(BODY_TAG) + 4 + contentLength;
                if (byteBuffer.limit() >= totalLength) {
                    return byteBuffer.limit();
                }
            }

            // 2. 如果是 HTTP POST报文
            // POST方法的multipart/form-data类型,且没有指定ContentLength,则需要使用--boundary--的结尾形式来判断
            if (bufferString.contains("multipart/form-data")
                    && boundaryLines.length == 1
                    && bufferString.trim().endsWith("--" + boundaryLines[0].replace("boundary=", "") + "--")) {
                return byteBuffer.limit();
            }

            // 3. 如果是 HTTP 响应报文 chunk
            // 则trim 后判断最后一个字符是否是 0
            if (isChunked && bufferString.trim().endsWith("\r\n0")) {
                return byteBuffer.limit();
            }

            // 4.是否是无报文体的简单请求报文(1.Header 中没有 ContentLength / 2.非 Chunked 报文形式)
            if (contentLengthLines.length == 0 && !isChunked) {
                return byteBuffer.limit();
            }
        }

		return -1;
	}

}
