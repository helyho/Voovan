package org.hocate.http.server.websocket;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.hocate.http.message.Request;
import org.hocate.http.message.packet.Header;

/**
 * WebSocket 工具类
 * @author helyho
 *
 */
public class WebSocketTools {
	public static boolean isWebSocketUpgrade(Request request) {
		Header header = request.header();
		if (header != null && header.contain("Connection") && header.get("Connection").equals("Upgrade")
				&& header.contain("Sec-WebSocket-Key")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String generateSecKey( String in ) {
		String seckey = in.trim();
		String acc = seckey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		MessageDigest sh1;
		try {
			sh1 = MessageDigest.getInstance( "SHA" );
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException( e );
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
