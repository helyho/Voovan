package org.voovan.http.websocket;

import org.voovan.tools.TByteBuffer;
import org.voovan.tools.log.Logger;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 * WebSocket帧解析类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class WebSocketFrame {
	private boolean		fin;
	private Opcode		opcode;
	private boolean		transfereMask;
	private ByteBuffer	frameData;
	private int errorCode = 0;

	public boolean isFin() {
		return fin;
	}

	public void setFin(boolean fin) {
		this.fin = fin;
	}

	public Opcode getOpcode() {
		return opcode;
	}

	public void setOpcode(Opcode opcode) {
		this.opcode = opcode;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public boolean isTransfereMask() {
		return transfereMask;
	}

	public void setTransfereMask(boolean transfereMask) {
		this.transfereMask = transfereMask;
	}

	public ByteBuffer getFrameData() {
		return frameData;
	}

	public void setFrameData(ByteBuffer frameData) {
		this.frameData = frameData;
	}

	/**
	 * 构建新的实例
	 * @param fin    fin 码
	 * @param opcode 操作码
	 * @param mask   掩码
	 * @param binary 二进制数据
	 * @param errorCode 错误码
	 * @return WebSocket 帧对象
	 */
	public static WebSocketFrame newInstance(boolean fin, Opcode opcode, boolean mask, ByteBuffer binary,int errorCode) {
		WebSocketFrame webSocketFrame = new WebSocketFrame();
		webSocketFrame.setFin(fin);
		webSocketFrame.setOpcode(opcode);
		webSocketFrame.setTransfereMask(mask);
		webSocketFrame.setFrameData(binary);
		webSocketFrame.setErrorCode(errorCode);
		return webSocketFrame;
	}

	/**
	 * 构建新的实例
	 * @param fin    fin 码
	 * @param opcode 操作码
	 * @param mask   掩码
	 * @param binary 二进制数据
	 * @return WebSocket 帧
	 */
	public static WebSocketFrame newInstance(boolean fin, Opcode opcode, boolean mask, ByteBuffer binary) {
		WebSocketFrame webSocketFrame = new WebSocketFrame();
		webSocketFrame.setFin(fin);
		webSocketFrame.setOpcode(opcode);
		webSocketFrame.setTransfereMask(mask);
		webSocketFrame.setFrameData(binary);
		return webSocketFrame;
	}

	/**
	 * 解析WebSocket报文
	 * 
	 * @param byteBuffer  字节缓冲对象
	 * @return  WebSocket 帧对象
	 */
	public static WebSocketFrame parse(ByteBuffer byteBuffer) {

	    //如果 byteBuffer 内没有数据则放弃
		if(!byteBuffer.hasRemaining()){
			return null;
		}

		int errorCode = 0;
		// 接受数据的大小
		int maxpacketsize = byteBuffer.remaining();
		// 期望数据包的实际大小
		int expectPackagesize = 2;
		if (maxpacketsize < expectPackagesize) {
			Logger.error("Expect package size error!");
			errorCode = 1002;
		}
		byte finByte = byteBuffer.get();
		boolean fin = finByte >> 8 != 0;
		byte rsv = (byte) ((finByte & ~(byte) 128) >> 4);
		if (rsv != 0) {
			Logger.error("RSV data error!");
			errorCode = 1002;
		}
		byte maskByte = byteBuffer.get();
		boolean mask = (maskByte & -128) != 0;
		int payloadlength = (byte) (maskByte & ~(byte) 128);
		Opcode opcode = toOpcode((byte) (finByte & 15));
		
		if(opcode == null){
			Logger.error("Opcode data error!");
			errorCode = 1002;
		}
		//“负载数据”的长度,以字节为单位:如果 0-125,这是负载长度。
		//如果 126, 之后的两字节解释为一个 16 位的无符号整数是负载长度。
		//如果 127,之后的 8￼字节解释为一个 64 位的无符号整数(最高有效位必须是 0)是负载长度。
		if (payloadlength == 126) {
			expectPackagesize += 2;
			byte[] sizebytes = new byte[3];
			sizebytes[1] = byteBuffer.get();
			sizebytes[2] = byteBuffer.get();
			payloadlength = new BigInteger(sizebytes).intValue();
		} else if(payloadlength==127) {
			expectPackagesize += 8;
			byte[] bytes = new byte[8];
			for (int i = 0; i < 8; i++) {
				bytes[i] = byteBuffer.get();
			}
			long length = new BigInteger(bytes).longValue();
			if (length <= Integer.MAX_VALUE) {
				payloadlength = (int) length;
			}
		}


		expectPackagesize += (mask ? 4 : 0);
		expectPackagesize += payloadlength;

		// 如果实际接受的数据小于数据包的大小则报错
		if (maxpacketsize < expectPackagesize) {
			Logger.error("Parse package size error!");
		}

		// 读取实际接受数据
		ByteBuffer payload = TByteBuffer.allocateDirect(payloadlength);
		if (mask) {
			byte[] maskskey = new byte[4];
			byteBuffer.get(maskskey);
			for (int i = 0; i < payloadlength; i++) {
				payload.put((byte) ((byte) byteBuffer.get() ^ (byte) maskskey[i % 4]));
			}
		} else {
			payload.put(byteBuffer);
		}
		payload.flip();
		WebSocketFrame webSocketFrame = WebSocketFrame.newInstance(fin, opcode, mask, payload, errorCode);
		return webSocketFrame;
	}

	/**
	 * 类型枚举
	 * 
	 * @author helyho
	 *
	 */
	public enum Opcode {
		CONTINUOUS, TEXT, BINARY, PING, PONG, CLOSING
	}

	/**
	 * 转换 WebSocket 报文类型为枚举类型
	 * 
	 * @param opcode
	 * @return
	 */
	private static Opcode toOpcode(byte opcode) {
		switch (opcode) {
		case 0:
			return Opcode.CONTINUOUS;
		case 1:
			return Opcode.TEXT;
		case 2:
			return Opcode.BINARY;
			// 3-7 are not yet defined
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

	/**
	 * opcode转换成 byte
	 * 
	 * @param opcode 操作码
	 * @return 字节
	 */
	private static byte fromOpcode(Opcode opcode) {
		if (opcode == Opcode.CONTINUOUS) {
			return 0;
		}
		else if (opcode == Opcode.TEXT) {
			return 1;
		}
		else if (opcode == Opcode.BINARY) {
			return 2;
		}
		else if (opcode == Opcode.CLOSING) {
			return 8;
		}
		else if (opcode == Opcode.PING) {
			return 9;
		}
		else if (opcode == Opcode.PONG) {
			return 10;
		}
		return -1;
	}

	/**
	 * 转换 long 为 byte
	 * 
	 * @param value     long 值
	 * @param bytecount 字节位数
	 * @return 转换 long 后的 byte
	 */
	private static byte[] toByteArray(long value, int bytecount) {
		byte[] buffer = new byte[bytecount];
		int highest = 8 * bytecount - 8;
		for (int i = 0; i < bytecount; i++) {
			buffer[i] = (byte) (value >>> (highest - 8 * i));
		}
		return buffer;
	}

	/**
	 * 将 WebSocketFrame 转换成 Bytebuffer 供 socket 通信用
	 * 
	 * 
	 * @return WebSocketFrame 转换后的 Bytebuffer
	 */
	public ByteBuffer toByteBuffer() {
		ByteBuffer data = this.getFrameData();
		if(data == null){
			data = ByteBuffer.allocate(0);
		}
		boolean mask = this.isTransfereMask(); 
		int sizebytes = data.remaining() <= 125 ? 1 : data.remaining() <= 65535 ? 2 : 8;
		ByteBuffer buf = TByteBuffer.allocateDirect(1 + (sizebytes > 1 ? sizebytes + 1 : sizebytes) + (mask ? 4 : 0) + data.remaining());
		byte optcode = fromOpcode(this.getOpcode());
		byte one = (byte) (this.isFin() ? -128 : 0);
		one |= optcode;
		buf.put(one);
		byte[] payloadlengthbytes = toByteArray(data.remaining(), sizebytes);

		if (sizebytes == 1) {
			buf.put((byte) ((byte) payloadlengthbytes[0] | (mask ? (byte) -128 : 0)));
		} else if (sizebytes == 2) {
			buf.put((byte) ((byte) 126 | (mask ? (byte) -128 : 0)));
			buf.put(payloadlengthbytes);
		} else if (sizebytes == 8) {
			buf.put((byte) ((byte) 127 | (mask ? (byte) -128 : 0)));
			buf.put(payloadlengthbytes);
		} else {
			Logger.error("Size representation not supported/specified");
		}

		if (mask) {
			ByteBuffer maskkey = TByteBuffer.allocateDirect(4);
			Random reuseableRandom = new Random();
			maskkey.putInt(reuseableRandom.nextInt());
			maskkey.flip();
			buf.put(maskkey);
			for (int i = 0; data.hasRemaining(); i++) {
				buf.put((byte) (data.get() ^ maskkey.get(i % 4)));
			}
			TByteBuffer.release(maskkey);
		} else{
			buf.put(data);
		}
		
		buf.flip();

		return buf;
	}
	
	@Override
	public String toString() {
		return "Framedata={FIN: " + this.isFin() + " , Mask: " + this.isTransfereMask() + " , OpCode: " + getOpcode() + " , Data: "
				+ TByteBuffer.toString(getFrameData())+ "}";
	}
}
