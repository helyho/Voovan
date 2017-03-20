package org.voovan.tools;

import org.voovan.tools.reflect.TReflect;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
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

	private long address;
	private Unsafe unsafe = TUnsafe.getUnsafe();
	private ByteBuffer byteBuffer;
	private int size;

	public ByteBufferChannel(int size) {
		try {
			byteBuffer = ByteBuffer.allocateDirect(size);
			address = TReflect.getFieldValue(byteBuffer, "address");
			size = 0;
		}catch (ReflectiveOperationException e){
			e.printStackTrace();
		}
	}

	public ByteBufferChannel() {
		try {
            byteBuffer = ByteBuffer.allocateDirect(256);
            address = TReflect.getFieldValue(byteBuffer, "address");
            size = 0;
		}catch (ReflectiveOperationException e){
			e.printStackTrace();
		}
	}

	/**
	 * 当前数组空闲的大小
	 * @return 当前数组空闲的大小
	 */
	public int free(){
		return byteBuffer.capacity() - size;
	}

	/**
	 * 返回当前分配的数组大小
	 * @return 当前分配的数组大小
	 */
	public int capacity(){
		return byteBuffer.capacity();
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
	 *        返回 0 到 size 的有效数据
	 * @return 缓冲区有效字节数组
	 */
	public byte[] array(){
		byte[] temp = new byte[size()];
		unsafe.copyMemory(null, address, temp, Unsafe.ARRAY_BYTE_BASE_OFFSET, size());
		return temp;
	}

	/**
	 * 获取缓冲区
	 *     返回 0 到 size 的有效数据
	 *     数据随时会变化,使用后下次使用建议重新获取,否则可能导致数据不全
	 * @return ByteBuffer 对象
	 */
	public ByteBuffer getByteBuffer(){
		return byteBuffer;
	}

	/**
	 * 重置通道
	 */
	public void flip(){
		byteBuffer.flip();
	}


	 /**
	 * 重置通道
	 */
	public void rewind(){
		byteBuffer.rewind();
	}


	/**
	 * 重置通道
	 */
	public void reset(){
		byteBuffer.clear();
	}

	/**
	 * 收缩通道
	 *      将通过getByteBuffer()方法获得 ByteBuffer 对象的操作同步到 ByteBufferChannel
	 *      如果不需要同步,则不用调用这个方法
	 * 		如果之前最后一次通过 getByteBuffer() 方法获得过 ByteBuffer,则使用这个 ByteBuffer 来收缩通道
	 *      将 (position 到 limit) 之间的数据 移动到 (0  到 limit - position) 其他情形将不做任何操作
	 *		所以 建议 getByteBuffer() 和 compact() 成对操作
	 */
	public void compact(){
		int position = byteBuffer.position();
		TByteBuffer.moveByteBufferData(byteBuffer, position*-1);
		size = size - position;
	}

	/**
	 * 缓冲区头部写入
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 写入的数据大小
	 */
	public synchronized int writeEnd(ByteBuffer src) {
		if(src==null){
			return -1;
		}

		byte[] srcByte = src.array();
		int writeSize = src.limit() - src.position();

		//是否扩容
		if(free() < writeSize) {
			int newSize = byteBuffer.capacity() + writeSize;
			address = TByteBuffer.reallocateDirectByteBuffer(byteBuffer, newSize);
		}

		byteBuffer.position(size);

		size = size+ writeSize;
		byteBuffer.limit(size);

		byteBuffer.put(src);
		byteBuffer.position(0);

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

		//是否扩容
		if (free() < writeSize) {
			int newSize = byteBuffer.capacity() + writeSize;
			address = TByteBuffer.reallocateDirectByteBuffer(byteBuffer, newSize);
		}

		//内容移动到 writeSize 之后
		TByteBuffer.moveByteBufferData(byteBuffer, writeSize);

		byteBuffer.put(src);
		size = size+ writeSize;
		byteBuffer.limit(size);
		byteBuffer.position(0);

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
		if (dst.remaining() > byteBuffer.remaining()) {
			readSize = byteBuffer.remaining();
		} else {
			readSize = dst.remaining();
		}

		if(readSize!=0) {
			while(dst.remaining()>0){
				dst.put(byteBuffer.get());
			}
			dst.position(readSize);
			TByteBuffer.moveByteBufferData(byteBuffer, -readSize);
			size = size - readSize;
			byteBuffer.limit(size);
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
		if (dst.remaining() > byteBuffer.remaining()) {
			readSize = byteBuffer.remaining();
		} else {
			readSize = dst.remaining();
		}

		if(readSize!=0) {
			byteBuffer.position(size - readSize);
			while(dst.remaining()>0){
				dst.put(byteBuffer.get());
			}
			size = size - readSize;
			byteBuffer.limit(size);
			byteBuffer.position(0);
		}

		dst.flip();

		return readSize;
	}

	/**
	 * 读取一行
	 * @return 字符串
	 */
	public String readLine() {
		String lineStr = "";
		int index = 0;
		while(byteBuffer.remaining()>0){
			index++;
			int singleChar = byteBuffer.get();
			if(singleChar==65535) {
				break;
			}
			else{
				if(singleChar == '\n'){
					break;
				}
			}
		}

		byteBuffer.position(0);

		ByteBuffer lineBuffer = ByteBuffer.allocate(index);

		int readSize = readHead(lineBuffer);

		if(readSize == index){
			lineStr = TByteBuffer.toString(lineBuffer);
		}

		return lineStr.isEmpty()?null:lineStr.trim();
	}


	/**
	 * 从 InputStream 读取一段,使用 byte数组 分割
	 * 		返回的 byte数组中不包含分割 byte 数组的内容
	 * @param splitByte 分割字节数组
	 * @return 字节数组
	 */
	public ByteBuffer readWithSplit(byte[] splitByte) {
		byte[] tempBytes = new byte[splitByte.length];
		int index = 0;
		while(byteBuffer.remaining()>index){
			if(byteBuffer.limit()-index >= tempBytes.length) {
				unsafe.copyMemory(null, address+index, tempBytes, Unsafe.ARRAY_BYTE_BASE_OFFSET, tempBytes.length);
					if (Arrays.equals(splitByte, tempBytes)) {
						break;
					}
			}
			index++;
		}

		byteBuffer.position(0);

		ByteBuffer resultBuffer = ByteBuffer.allocate(index);

		int readSize = readHead(resultBuffer);

		//跳过分割符
		readHead(ByteBuffer.allocate(tempBytes.length));

		return resultBuffer.limit()==0?null:resultBuffer;
	}



}
