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
	private int bodyTagIndex= -1;

	private int contentLength = -1;
    boolean isChunked = false;


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

    private void getBodyTagIndex(ByteBuffer byteBuffer){
        byte[] buffer = byteBuffer.array();
        StringBuilder stringBuilder = new StringBuilder();
        String httpHead = null;
        for(int x=0;x<buffer.length;x++){
            if(buffer[x] == '\r' && buffer[x+1] == '\n' && buffer[x+2] == '\r' && buffer[x+3] == '\n'){
                bodyTagIndex = x;
                httpHead = stringBuilder.toString();
                break;
            }else{
                stringBuilder.append((char)buffer[x]);
            }
        }

        if(httpHead !=null && isHttpHead(httpHead)) {

            String[] contentLengthLines = TString.searchByRegex(httpHead, "Content-Length: \\d+");
            if (contentLengthLines.length > 1) {
                contentLength = Integer.parseInt(contentLengthLines[0].split(" ")[1].trim());
            }

            isChunked = httpHead.contains("chunked");

        }else{
            bodyTagIndex = -1;
        }
    }

    private boolean isHttpHead(String str){
        //判断是否是 HTTP 头
        int firstLineIndex = str.indexOf("\r\n");
        if(firstLineIndex != -1) {
            String firstLine = str.substring(0, firstLineIndex);
            if (TString.regexMatch(firstLine, "HTTP\\/\\d\\.\\d\\s\\d{3}\\s.*") <= 0) {
                if (TString.regexMatch(firstLine, "^[A-Z]*\\s.*\\sHTTP\\/\\d\\.\\d") <= 0) {
                    return false;
                }
            }
        }
        return true;
    }

	public int isHttpFrame(ByteBuffer byteBuffer) {
	    try{
            if(bodyTagIndex==-1) {
                getBodyTagIndex(byteBuffer);
            }

            if(bodyTagIndex!=-1) {

                if (bodyTagIndex != -1) {
                    // 1.包含 content Length 的则通过获取 contentLenght 来计算报文的总长度,长度相等时,返回成功
                    if (contentLength != -1) {
                        int totalLength = bodyTagIndex + 4 + contentLength;
                        if (byteBuffer.limit() >= totalLength) {
                            return byteBuffer.limit();
                        }
                    }

                    // 2. 如果是 HTTP 响应报文 chunk
                    // 则trim 后判断最后一个字符是否是 0
                    if (isChunked) {
                        byteBuffer.position(byteBuffer.limit() - 7);
                        byte[] tailBytes = new byte[7];
                        byteBuffer.get(tailBytes);
                        byteBuffer.position(0);
                        String tailStr = new String(tailBytes, "UTF-8");
                        if("\r\n0\r\n\r\n".equals(tailStr) || tailStr.endsWith("\r\n0")) {
                            return byteBuffer.limit();
                        }
                    }

                    // 3.是否是无报文体的简单请求报文(1.Header 中没有 ContentLength / 2.非 Chunked 报文形式)
                    if (contentLength == -1 && !isChunked) {
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
