package org.voovan.network.messagesplitter;

import org.voovan.http.websocket.WebSocketTools;
import org.voovan.network.IoSession;
import org.voovan.network.MessageSplitter;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TStream;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Executable;
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
	private int bodyTagIndex = -1;


	@Override
	public int canSplite(IoSession session, ByteBuffer byteBuffer) {

		if(byteBuffer.limit()==0){
			return -1;
		}

		result = isHttpFrame(byteBuffer);

	    if (result==-1 && "WebSocket".equals(session.getAttribute("Type")) ) {
			result = WebSocketTools.isWebSocketFrame(byteBuffer);
		}

		if(result!=-1){
            bodyTagIndex = -1;
        }

		return result;
	}

	public int getBodyTagIndex(ByteBuffer byteBuffer){
        byte[] buffer = byteBuffer.array();
        for(int x=0;x<buffer.length;x++){
            if(buffer[x] == '\r' && buffer[x+1] == '\n' && buffer[x+2] == '\r' && buffer[x+3] == '\n'){
                return x;
            }
        }
        return -1;
    }

	public int isHttpFrame(ByteBuffer byteBuffer) {
	    try{
            if(bodyTagIndex==-1) {
                bodyTagIndex = getBodyTagIndex(byteBuffer);
            }

            if(bodyTagIndex!=-1) {

                ByteArrayInputStream bufferStream = new ByteArrayInputStream(byteBuffer.array());
                String bufferString = new String(TStream.read(bufferStream, bodyTagIndex), "UTF-8");

                //判断是否是 HTTP 头
                String firstLine = bufferString.substring(0, bufferString.indexOf("\r\n"));
                if (TString.regexMatch(firstLine, "HTTP\\/\\d\\.\\d\\s\\d{3}\\s.*") <= 0) {
                    if (TString.regexMatch(firstLine, "^[A-Z]*\\s.*\\sHTTP\\/\\d\\.\\d") <= 0) {
                        return -1;
                    }
                }

                //读取必要参数
                String[] boundaryLines = TString.searchByRegex(bufferString, "boundary=[^ \\r\\n]+");
                String[] contentLengthLines = TString.searchByRegex(bufferString, "Content-Length: \\d+");
                boolean isChunked = bufferString.contains("chunked");

                if (bodyTagIndex != -1) {
                    // 1.包含 content Length 的则通过获取 contentLenght 来计算报文的总长度,长度相等时,返回成功
                    if (contentLengthLines.length == 1) {
                        int contentLength = Integer.parseInt(contentLengthLines[0].split(" ")[1].trim());
                        int totalLength = bodyTagIndex + 4 + contentLength;
                        if (byteBuffer.limit() >= totalLength) {
                            return byteBuffer.limit();
                        }
                    }

                    // 2. 如果是 HTTP 响应报文 chunk
                    // 则trim 后判断最后一个字符是否是 0
                    if (isChunked) {
                        bufferStream.skip(bufferStream.available()-7);
                        byte[] tailBytes = new byte[7];
                        bufferStream.read(tailBytes);
                        String tailStr = new String(tailBytes, "UTF-8");
                        if("\r\n0\r\n\r\n".equals(tailStr) || tailStr.endsWith("\r\n0")) {
                            return byteBuffer.limit();
                        }
                    }

                    // 3.是否是无报文体的简单请求报文(1.Header 中没有 ContentLength / 2.非 Chunked 报文形式)
                    if (contentLengthLines.length == 0 && !isChunked) {
                        return byteBuffer.limit();
                    }
                }
            }
	    }catch(Exception e){
            Logger.error(e);
        }
        return -1;
	}

}
