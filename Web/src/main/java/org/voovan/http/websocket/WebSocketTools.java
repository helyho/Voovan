package org.voovan.http.websocket;

import org.voovan.http.message.Request;
import org.voovan.http.message.packet.Header;
import org.voovan.tools.security.TBase64;
import org.voovan.tools.log.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * WebSocket 工具类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebSocketTools {

	private WebSocketTools(){

	}

	/**
	 * s是否是 websocket 升级协议
	 * @param request Http 请求对象
	 * @return 是否是 Websocket 升级协议
     */
	public static boolean isWebSocketUpgrade(Request request) {
		Header header = request.header();
		return header != null && "websocket".equalsIgnoreCase(header.get("Upgrade"))
				&& header.contain("Sec-WebSocket-Key");
	}
	
	/**
	 * 生成协议升级的 KEY
	 * @param in 协议参数
	 * @return 协议升级的 KEY
	 */
	public static String generateSecKey( String in ) {
		String seckey = in.trim();
		String acc = seckey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		MessageDigest sh1 = null;
		try {
			sh1 = MessageDigest.getInstance( "SHA" );
			return TBase64.encode( sh1.digest(acc.getBytes()) );
		} catch ( NoSuchAlgorithmException e ) {
			Logger.error("No Such Algorithm.", e);
			return null;
		}

	}

	/**
	 * 将 int 转换成 byte[]
	 * @param iSource     int 值
	 * @param iArrayLen   数组长度
     * @return int 转换后的 byte[]
     */
	public static byte[] intToByteArray(int iSource, int iArrayLen) {
	    byte[] bLocalArr = new byte[iArrayLen];
	    for (int i = 0; (i < 4) && (i < iArrayLen); i++) {
	        bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
	    }
	    return bLocalArr;
	}

	/**
	 * byte 转换成 int
	 * @param bRefArr byte 数组
	 * @return int 值
     */
	public static int byteToInt(byte[] bRefArr) {
	    int iOutcome = 0;
	    byte bLoop;

	    for (int i = 0; i < bRefArr.length; i++) {
	        bLoop = bRefArr[i];
	        iOutcome += (bLoop & 0xFF) << (8 * i);
	    }
	    return iOutcome;
	}
}
