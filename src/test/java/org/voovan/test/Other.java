package org.voovan.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.voovan.http.server.websocket.WebSocketFrame;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

public class Other {
	public static void main(String[] args) throws Exception {
		Integer s= 1025;
		Logger.simple(s.byteValue());
		ByteBuffer byteBuffer = ByteBuffer.allocate(0);
		Logger.simple(byteBuffer.hasRemaining());
		
		
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		byteOutputStream.write("bingo".getBytes());
		Logger.info(byteOutputStream.toByteArray().length);
		
		Logger.simple(ClassLoader.getSystemClassLoader().getClass().getName());
		
		Logger.simple(System.getProperty("user.dir"));
		String regex = ":[^/]+";
		Logger.simple("/test/:username_a/:id".replaceAll(regex, "[^/?]+"));
		
		
		File fileClasses = new File("/Users/helyho/Work/Java/try/bin/");
		
		TEnv.LoadJars("/Users/helyho/Work/Java/Ozologo/WEBAPP/WEB-INF/lib");
		TEnv.loadBinary(fileClasses.getPath());
		Class.forName("trySomething");
		
		byte[] bytes = new  byte[]{-127, -123, 19, 95, 33, 95, 123, 58, 77, 51, 124};
		
		ByteBuffer bytebuffer = ByteBuffer.allocate(bytes.length);
		bytebuffer.put(bytes);
		bytebuffer.rewind();
		translateSingleFrame(bytebuffer);
		
		WebSocketFrame webSocketFrame =new  WebSocketFrame();
		webSocketFrame.setFin(true);
		webSocketFrame.setOpcode(org.voovan.http.server.websocket.WebSocketFrame.Opcode.TEXT);
		webSocketFrame.setFrameData(ByteBuffer.wrap("helyho".getBytes()));
		createBinaryFrame(webSocketFrame);
	}
	
	public enum Opcode {
		CONTINUOUS, TEXT, BINARY, PING, PONG, CLOSING
	}
	
	private static Opcode toOpcode( byte opcode ) {
		switch ( opcode ) {
			case 0:
				return Opcode.CONTINUOUS;
			case 1:
				return Opcode.TEXT;
			case 2:
				return Opcode.BINARY;
			case 8:
				return Opcode.CLOSING;
			case 9:
				return Opcode.PING;
			case 10:
				return Opcode.PONG;
			default:
				return null;
		}
	}
	
	public static void translateSingleFrame( ByteBuffer buffer ) {
		//�������ݵĴ�С
		int maxpacketsize = buffer.remaining();
		// �������ݰ���ʵ�ʴ�С
		int expectPackagesize = 2;
		if( maxpacketsize < expectPackagesize ){
			Logger.error("Package size error!");
			return;
		}
		byte finByte = buffer.get();
		boolean FIN = finByte >> 8 != 0;
		byte rsv = (byte) ( ( finByte & ~(byte) 128 ) >> 4 );
		if(rsv!=0){
			Logger.error("Rsv data error!");
			return;
		}
		byte maskByte = buffer.get();
		boolean mask = ( maskByte & -128 ) != 0;
		int payloadlength = (byte) ( maskByte & ~(byte) 128 );
		Opcode optcode = toOpcode( (byte) ( finByte & 15 ) );

		if( !FIN ) {
			if( optcode == Opcode.PING || optcode == Opcode.PONG || optcode == Opcode.CLOSING ) {
				Logger.info("Invalid frame");
				return;
			}
		}

		if( payloadlength >= 0 && payloadlength <= 125 ) {
		} else {
			if( optcode == Opcode.PING || optcode == Opcode.PONG || optcode == Opcode.CLOSING ) {
				Logger.info("Invalid frame");
				return;
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

		//���������ܵ�����С�����ݰ��Ĵ�С�򱨴�
		if( maxpacketsize < expectPackagesize ){
			Logger.error("Package size error!");
			return;
		}

		//��ȡʵ�ʽ�������
		ByteBuffer payload = ByteBuffer.allocate( payloadlength );
		if( mask ) {
			byte[] maskskey = new byte[ 4 ];
			buffer.get( maskskey );
			for( int i = 0 ; i < payloadlength ; i++ ) {
				payload.put( (byte) ( (byte) buffer.get() ^ (byte) maskskey[ i % 4 ] ) );
			}
		} else {
			payload.put( buffer.array(), buffer.position(), payload.limit() );
			buffer.position( buffer.position() + payload.limit() );
		}

		System.out.println("FIN: "+FIN);
		System.out.println("OptCode: "+optcode);
		System.out.println("payload: "+new String(payload.array()));
	}
	
	public static ByteBuffer createBinaryFrame( WebSocketFrame webSocketFrame ) {

		ByteBuffer pay = webSocketFrame.getFrameData();
		ByteBuffer b = ByteBuffer.allocate( pay.remaining() + 2 );
		b.put( (byte) 0x00 );//��ʼ
		pay.mark();
		b.put( pay );
		pay.reset();
		b.put( (byte) 0xFF );//����
		b.flip();
		return b;
	}
}
