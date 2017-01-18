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
	private ByteBuffer byteBuffer;

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
	 * 获取缓冲区有效字节数组的一个拷贝
	 *        修改这个数组将不会影响当前对象
	 *        返回 0->size 的有效数据
	 * @return 缓冲区有效字节数组
	 */
	public byte[] array(){
		return Arrays.copyOfRange(buffer, 0, size);
	}

	/**
	 * 获取缓冲区
	 *     返回 0->size 的有效数据
	 *     数据随时会变化,使用后下次使用建议重新获取,否则可能导致数据不全
	 * @return ByteBuffer 对象
	 */
	public ByteBuffer getByteBuffer(){
		byteBuffer = ByteBuffer.wrap(buffer, 0, size);
		return byteBuffer;
	}

	/**
	 * 重置通道
	 */
	public void reset(){
		size = 0;
	}

	/**
	 * 收缩通道
	 *      将通过getByteBuffer()方法获得 ByteBuffer 对象的操作同步到 ByteBufferChannel
	 *      如果不需要同步,则不用调用这个方法
	 * 		如果之前最后一次通过getByteBuffer()方法获得过 ByteBuffer,则使用这个 Byte 来收缩通道
	 *      将 (position -> limit) 之间的数据 移动到 (0 -> limit - position) 其他情形将不做任何操作
	 *		所以 建议 getByteBuffer() 和 compact() 成对操作
	 */
	public synchronized void compact(){
		if(byteBuffer!=null) {
			size = size - byteBuffer.position();
			if(size > 0) {
				System.arraycopy(buffer, byteBuffer.position(), buffer, 0, size);
			}else{
				size = 0;
			}

		}
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

		if(byteBuffer!=null && size <= byteBuffer.capacity()){
			byteBuffer.limit(size);
		}
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

		if(byteBuffer!=null && size <= byteBuffer.capacity()){
			byteBuffer.position(writeSize);
			byteBuffer.limit(size);
		}

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
		if(byteBuffer!=null){

			int newPosition = byteBuffer.position() - readSize;
			if(newPosition > 0) {
				byteBuffer.position(newPosition);
			}

			int newSize = size - readSize;
			if(newSize > 0 && size <= byteBuffer.capacity()) {
				byteBuffer.limit(newSize < 0 ? 0 : newSize);
			}
		}

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

		if(byteBuffer!=null && size <= byteBuffer.capacity()){
			byteBuffer.limit(size - readSize);
		}

		return readSize;
	}
}
