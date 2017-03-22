package org.voovan.tools;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import sun.misc.Unsafe;

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
        this.byteBuffer = ByteBuffer.allocateDirect(size);
		byteBuffer.limit(0);
        this.address = address();
        size = 0;
	}

	public ByteBufferChannel() {
        this.byteBuffer = ByteBuffer.allocateDirect(256);
		byteBuffer.limit(0);
        this.address = address();
        size = 0;
	}

	/**
	 * 获取当前数据的内存起始地址
	 * @return 当前数据的内存起始地址
	 */
	public long address(){
		try {
			return TReflect.getFieldValue(byteBuffer, "address");
		}catch (ReflectiveOperationException e){
			Logger.error("ByteBufferChannel.address() Error: "+e.getMessage(), e);
		}
		return -1;
	}

	/**
	 * 当前数组空闲的大小
	 * @return 当前数组空闲的大小
	 */
	public int available(){
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
	 * 清空通道
	 */
	public void clear(){
		byteBuffer.clear();
	}


	/**
	 * 获取某个偏移量位置的 byte 数据
	 *     该操作不会导致通道内的数据发生变化
	 * @param offset 偏移量位置的
	 * @return byte 数据
	 */
	public synchronized byte get(int offset) throws IndexOutOfBoundsException {
		if(offset >= 0 && offset <= size) {
            return unsafe.getByte(address + offset);
        } else {
            throw new IndexOutOfBoundsException();
        }
	}


	/**
	 * 获取某个偏移量位置的 byte 数据数组
	 *     该操作不会导致通道内的数据发生变化
	 * @param offset  偏移量
	 * @param dst     目标数组
	 * @param length  长度
	 */
	public synchronized void get(int offset, byte[] dst, int length) throws IndexOutOfBoundsException {

		if(offset >= 0 && length <= size - offset) {
			unsafe.copyMemory(null, address + offset, dst, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}



	/**
	 * 获取某个偏移量位置的 byte 数据数组
	 *     该操作不会导致通道内的数据发生变化
	 * @param dst     目标数组
	 */
	public synchronized void get(byte[] dst){
		unsafe.copyMemory(null, address, dst, Unsafe.ARRAY_BYTE_BASE_OFFSET, dst.length);
	}

	/**
	 * 收缩通道
	 *      将通过getByteBuffer()方法获得 ByteBuffer 对象的操作同步到 ByteBufferChannel
	 *      如果不需要同步,则不用调用这个方法
	 * 		如果之前最后一次通过 getByteBuffer() 方法获得过 ByteBuffer,则使用这个 ByteBuffer 来收缩通道
	 *      将 (position 到 limit) 之间的数据 移动到 (0  到 limit - position) 其他情形将不做任何操作
	 *		所以 建议 getByteBuffer() 和 compact() 成对操作
	 */
	public synchronized boolean compact(){
		int position = byteBuffer.position();
		if(TByteBuffer.moveData(byteBuffer, position*-1)) {
			byteBuffer.position(0);
			size = size - position;
			byteBuffer.limit(size);
			return true;
		}

		return false;
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

		if(writeSize > 0){
            //是否扩容
            if(available() < writeSize) {
                int newSize = byteBuffer.capacity() + writeSize;
                if(TByteBuffer.reallocate(byteBuffer, newSize)){
                    this.address = address();
                }
            }


            byteBuffer.position(size);

            size = size+ writeSize;
            byteBuffer.limit(size);

            byteBuffer.put(src);
            byteBuffer.position(0);

            return writeSize;
		}

		return 0;
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

		if(writeSize>0){
            //是否扩容
            if (available() < writeSize) {
                int newSize = byteBuffer.capacity() + writeSize;
                if(TByteBuffer.reallocate(byteBuffer, newSize)){
                    this.address = address();
                }
            }

            //内容移动到 writeSize 之后
            if(TByteBuffer.moveData(byteBuffer, writeSize)){
                byteBuffer.position(0);
                byteBuffer.put(src);
                size = size+ writeSize;
                byteBuffer.limit(size);
                byteBuffer.position(0);

                return writeSize;
            }
		}

		return 0;
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
			for(int i=0;i<readSize;i++){
				dst.put(byteBuffer.get());
			}
			if(TByteBuffer.moveData(byteBuffer, -readSize)) {
				byteBuffer.position(0);
				size = size - readSize;
				byteBuffer.limit(size);
				dst.flip();
				return readSize;
			}else{
				dst.reset();
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
			byteBuffer.position(size - readSize);
			for(int i=0;i<readSize;i++){
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
	 * 查找特定 byte 标识的位置
	 *     byte 标识数组第一个字节的索引位置
	 * @param mark byte 标识数组
	 * @return 第一个字节的索引位置
	 */
	public int indexOf(byte[] mark){
		if(size == 0){
			return -1;
		}

		int index = -1;
		byte[] tmp = new byte[mark.length];
		for(int offset = 0;offset <= byteBuffer.remaining() - mark.length; offset++){
            get(offset, tmp, tmp.length);
            if(Arrays.equals(mark, tmp)){
            	index = offset;
                break;
            }
		}

		return index;
	}

	/**
	 * 读取一行
	 * @return 字符串
	 */
	public String readLine() {
		if(size == 0){
			return null;
		}


		String lineStr = "";
		int index = indexOf("\n".getBytes());

		byteBuffer.position(0);

		ByteBuffer lineBuffer = ByteBuffer.allocate(index + 1);

		int readSize = readHead(lineBuffer);

		if(readSize == index + 1){
			lineStr = TByteBuffer.toString(lineBuffer);
		}

		return lineStr.isEmpty()?null:lineStr;
	}


	/**
	 * 从 InputStream 读取一段,使用 byte数组 分割
	 * 		返回的 byte数组中不包含分割 byte 数组的内容
	 * @param splitByte 分割字节数组
	 * @return 字节数组
	 */
	public ByteBuffer readWithSplit(byte[] splitByte) {
		int index = indexOf(splitByte);

		if(size == 0){
			return ByteBuffer.allocate(0);
		}

		if(index == 0){
			byteBuffer.position(splitByte.length);
			compact();
			index = indexOf(splitByte);
		}

		if(index == -1){
			index = size;
		}

		ByteBuffer resultBuffer = ByteBuffer.allocate(index);
		int readSize = readHead(resultBuffer);

		//跳过分割符
		readHead(ByteBuffer.allocate(splitByte.length));

		return resultBuffer;
	}
}
