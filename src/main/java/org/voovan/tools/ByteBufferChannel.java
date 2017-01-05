package org.voovan.tools;

import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * ByteBuffer双向通道
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ByteBufferChannel implements ByteChannel {

	private ByteBuffer buffer;

	public ByteBufferChannel() {
		buffer = ByteBuffer.allocate(0);
	}

	/**
	 * 重置
	 */
	public  void reset() {
		synchronized(buffer) {
			buffer = ByteBuffer.allocate(0);
		}
	}

	/**
	 * 当前数据大小
	 * @return 数据大小
	 */
	public int size(){
		return buffer.limit();
	}

	/**
	 * 当前数据缓冲区
	 * @return 数据缓冲区
	 */
	public ByteBuffer getBuffer(){
		return buffer;
	}



	/**
	 * 没有作用永远返回 true
	 */
	@Override
	public boolean isOpen() {
		return true;
	}

	/**
	 * 没有作用
	 */
	@Override
	public void close() throws IOException {

	}

	@Override
	public synchronized int write(ByteBuffer src) throws IOException {
		int writeSize = src.remaining();
		if(writeSize!=0){
			int newSize = buffer.remaining()+src.remaining();
			ByteBuffer tempBuffer = ByteBuffer.allocate(newSize);
			tempBuffer.put(buffer);
			tempBuffer.put(src);
			buffer = tempBuffer;
			buffer.flip();

		}
		return writeSize;
	}

	@Override
	public synchronized int read(ByteBuffer dst) throws IOException {
		int readSize = 0;

		//确定读取大小
		if (dst.remaining() > buffer.remaining()) {
			readSize = buffer.remaining();
		} else {
			readSize = dst.remaining();
		}

		if (readSize > 0) {
			try {
				byte[] tempBytes = new byte[readSize];
				buffer.get(tempBytes,0,readSize);
				dst.put(tempBytes);
			} catch (Exception e) {
				Logger.simple("error");
			}

			buffer.position(readSize);

			if(buffer.remaining()>0) {
				byte[] tempBytes = new byte[buffer.remaining()];
				buffer.get(tempBytes, 0, buffer.remaining());
				buffer = ByteBuffer.allocate(tempBytes.length);
				buffer.put(tempBytes);
				buffer.flip();
			}else{
				buffer = ByteBuffer.allocate(0);
			}
		}
		dst.flip();

		return readSize;
	}
}
