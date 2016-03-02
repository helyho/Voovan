package org.voovan.http.server.websocket;

import org.voovan.http.message.Request;
import org.voovan.http.message.packet.Header;
import org.voovan.tools.log.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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
	public static boolean isWebSocketUpgrade(Request request) {
		Header header = request.header();
		return header != null && "websocket".equalsIgnoreCase(header.get("Upgrade"))
				&& header.contain("Sec-WebSocket-Key");
	}
	
	/**
	 * 生成协议升级的 KEY
	 * @param in
	 * @return
	 */
	public static String generateSecKey( String in ) {
		String seckey = in.trim();
		String acc = seckey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		MessageDigest sh1 = null;
		try {
			sh1 = MessageDigest.getInstance( "SHA" );
		} catch ( NoSuchAlgorithmException e ) {
			Logger.error("No Such Algorithm.", e);
		}
		return Base64.getEncoder().encodeToString( sh1.digest(acc.getBytes()) );
	}
	
	public static byte[] intToByteArray(int iSource, int iArrayLen) {
	    byte[] bLocalArr = new byte[iArrayLen];
	    for (int i = 0; (i < 4) && (i < iArrayLen); i++) {
	        bLocalArr[i] = (byte) (iSource >> 8 * i & 0xFF);
	    }
	    return bLocalArr;
	}
	
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
