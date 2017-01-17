package org.voovan.tools;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * ByteBuffer双向通道
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ByteBufferChannel {

	private byte[] buffer;
	private int size;

	public ByteBufferChannel(int size) {
		buffer = new byte[size];
		size = 0;
	}

	public ByteBufferChannel() {
		buffer = new byte[256];
		size = 0;
	}


	/**
	 * 当前数组空闲的大小
	 * @return 当前数组空闲的大小
	 */
	public int free(){
		return buffer.length - size;
	}

	/**
	 * 返回当前分配的数组大小
	 * @return 当前分配的数组大小
	 */
	public int capacity(){
		return buffer.length;
	}

	/**
	 * 当前数据大小
	 * @return 数据大小
	 */
	public int size(){
		return size;
	}

	/**
	 * 获取缓冲区有效字节数组
	 *        返回 0->size 的有效数据
	 * @return 缓冲区有效字节数组
	 */
	public byte[] array(){
		return Arrays.copyOfRange(buffer, 0, size);
	}

	/**
	 * 获取缓冲区
	 *     返回 0->size 的有效数据
	 *     通道会被重置
	 * @return ByteBuffer 对象
	 */
	public ByteBuffer getByteBuffer(){
		ByteBuffer byteBuffer = ByteBuffer.wrap(array());
		reset();
		return byteBuffer;
	}

	/**
	 * 重置通道
	 */
	public void reset(){
		size = 0;
	}

	/**
	 * 缓冲区头部写入
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public synchronized int writeEnd(ByteBuffer src) {
		if(src==null){
			return -1;
		}

		byte[] srcByte = src.array();
		int writeSize = src.limit() - src.position();

		if(free() < writeSize) {
			buffer = Arrays.copyOf(buffer, buffer.length + writeSize);
		}

		System.arraycopy(srcByte, src.position() , buffer , size, writeSize);
		size = size + writeSize;
		return writeSize;
	}

	/**
	 * 缓冲区尾部写入
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public synchronized int writeHead(ByteBuffer src) {
		if (src == null) {
			return -1;
		}

		byte[] srcByte = src.array();
		int writeSize = src.limit() - src.position();

		if (free() < writeSize) {
			buffer = Arrays.copyOf(buffer, buffer.length + writeSize);
		}

		if (size != 0) {
			System.arraycopy(buffer, 0, buffer, writeSize, size);
		}

		System.arraycopy(srcByte, src.position() , buffer , 0, writeSize);
		size = size + writeSize;
		return writeSize;
	}

	/**
	 * 从缓冲区头部读取数据
	 * @param dst 需要读入数据的缓冲区ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public synchronized int readHead(ByteBuffer dst) {
		if(dst==null){
			return -1;
		}

		int readSize = 0;

		//确定读取大小
		if (dst.remaining() > size) {
			readSize = size;
		} else {
			readSize = dst.remaining();
		}

		if(readSize!=0) {
			System.arraycopy(buffer, 0, dst.array(), 0, readSize);

			size = size - readSize;

			System.arraycopy(buffer, readSize, buffer, 0, size);
			dst.position(readSize);
		}

		dst.flip();

		return readSize;
	}

	/**
	 * 从缓冲区尾部读取数据
	 * @param dst 需要读入数据的缓冲区ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public synchronized int readEnd(ByteBuffer dst) {
		if(dst==null){
			return -1;
		}

		int readSize = 0;

		//确定读取大小
		if (dst.remaining() > size) {
			readSize = size;
		} else {
			readSize = dst.remaining();
		}

		if(readSize!=0) {

			size = size - readSize;

			System.arraycopy(buffer, size, dst.array(), 0, readSize);
			dst.position(readSize);

			System.arraycopy(buffer, 0, buffer, 0, size);

		}

		dst.flip();

		return readSize;
	}
}
