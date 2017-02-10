package org.voovan.http.websocket;

import org.voovan.http.message.Request;
import org.voovan.http.message.packet.Header;
import org.voovan.tools.TBase64;
import org.voovan.tools.log.Logger;

import java.math.BigInteger;
import java.nio.ByteBuffer;
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

	/**
	 * 判断缓冲区中的数据是否是一个 WebSocket 帧
	 * @param buffer 缓冲区对象
	 * @return WebSocket 帧报文长度,-1不是WebSocket 帧, 大于0 返回的 WebSocket 的长度
	 */
	public static int isWebSocketFrame(ByteBuffer buffer) {
		// 接受数据的大小
		int maxpacketsize = buffer.remaining();
		// 期望数据包的实际大小
		int expectPackagesize = 2;
		if (maxpacketsize < expectPackagesize) {
			return -1;
		}
		byte finByte = buffer.get();
		boolean fin = finByte >> 8 != 0;
		byte rsv = (byte) ((finByte & ~(byte) 128) >> 4);
		if (rsv != 0) {
			return -1;
		}
		byte maskByte = buffer.get();
		boolean mask = (maskByte & -128) != 0;
		int payloadlength = (byte) (maskByte & ~(byte) 128);
		int optcode = (byte) (finByte & 15);

		if (!fin) {
			if (optcode == 9 || optcode == 10 || optcode == 8) {
				return -1;
			}
		}

		if (payloadlength >= 0 && payloadlength <= 125) {
		} else {
			if (optcode == 9 || optcode == 10 || optcode == 8) {
				return -1;
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
			return -1;
		} else {
			buffer.position(0);
			return buffer.remaining();
		}
	}
}
