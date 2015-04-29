package org.voovan.network.messageparter;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.voovan.network.IoSession;
import org.voovan.network.MessageParter;
import org.voovan.tools.TString;

public class HttpMessageParter implements MessageParter {

	private static final String BODY_TAG= "\r\n\r\n";
	
	@Override
	public boolean canPartition(IoSession session, byte[] buffer, int elapsedtime) {
		
		if(isHttpFrame(buffer)){
			return true;
		}else if(isWebSocketFrame(ByteBuffer.wrap(buffer))){
			return true;
		}

		return false;
	}
	
	public static boolean isHttpFrame(byte[] buffer){
		String bufferString = new String(buffer);
		// 包含\r\n\r\n,这个时候 Content-Length 可能存在
		if (bufferString.contains(BODY_TAG)) {
			String[] contentLengthLines = TString.searchByRegex(bufferString, "Content-Length: .+[^\\r\\n]");
			// 1.包含 content Length 的则通过获取 contentLenght 来计算报文的总长度,长度相等时,返回成功
			if (contentLengthLines.length == 1) {
				int contentLength = Integer.valueOf(contentLengthLines[0].split(" ")[1]);
				int totalLength = bufferString.indexOf(BODY_TAG) + 4 + contentLength;
				if (buffer.length == totalLength) {
					return true;
				}
			}
			// 2.不包含 ContentLength头的报文,则通过\r\n\r\n进行结尾判断,
			else if (bufferString.endsWith(BODY_TAG)) {
				// 3.分段传输的 POST 请求报文的报文头和报文题结束标识都是\r\n\r\n,所以还要判断出现两次的\r\n\r\n 的位置不同说明报文加载完成
				if (bufferString.indexOf(BODY_TAG) != bufferString.lastIndexOf(BODY_TAG)) {
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
	
	public static boolean isWebSocketFrame( ByteBuffer buffer ) {
		//接受数据的大小
		int maxpacketsize = buffer.remaining();
		// 期望数据包的实际大小
		int expectPackagesize = 2;
		if( maxpacketsize < expectPackagesize ){
			return false;
		}
		byte finByte = buffer.get();
		boolean fin = finByte >> 8 != 0;
		byte rsv = (byte) ( ( finByte & ~(byte) 128 ) >> 4 );
		if(rsv!=0){
			return false;
		}
		byte maskByte = buffer.get();
		boolean mask = ( maskByte & -128 ) != 0;
		int payloadlength = (byte) ( maskByte & ~(byte) 128 );
		int optcode =  (byte) ( finByte & 15 ) ;

		if( !fin ) {
			if( optcode == 9 || optcode == 10 || optcode == 8 ) {
				return false;
			}
		}

		if( payloadlength >= 0 && payloadlength <= 125 ) {
		} else {
			if( optcode == 9 || optcode == 10 || optcode == 8 ) {
				return false;
			}
			if( payloadlength == 126 ) {
				expectPackagesize += 2; 
				byte[] sizebytes = new byte[ 3 ];
				sizebytes[ 1 ] = buffer.get();
				sizebytes[ 2 ] = buffer.get();
				payloadlength = new BigInteger( sizebytes ).intValue();
			} else {
				expectPackagesize += 8; 
				byte[] bytes = new byte[ 8 ];
				for( int i = 0 ; i < 8 ; i++ ) {
					bytes[ i ] = buffer.get();
				}
				long length = new BigInteger( bytes ).longValue();
				if( length <= Integer.MAX_VALUE ) {
					payloadlength = (int) length;
				}
			}
		}

		expectPackagesize += ( mask ? 4 : 0 );
		expectPackagesize += payloadlength;

		//如果世界接受的数据小于数据包的大小则报错
		if( maxpacketsize < expectPackagesize ){
			return false;
		}else{
			return true;
		}
	}

}
