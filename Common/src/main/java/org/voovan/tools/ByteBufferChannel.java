package org.voovan.tools;

import org.voovan.tools.exception.MemoryReleasedException;
import org.voovan.tools.log.Logger;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

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
	private final ByteBuffer emptyByteBuffer = ByteBuffer.allocateDirect(0);

	private volatile AtomicLong address = new AtomicLong(0);
	private Unsafe unsafe = TUnsafe.getUnsafe();
	private ByteBuffer byteBuffer;
	private int size;
	private ReentrantLock lock;

	/**
	 * 构造函数
	 * @param capacity 分配的容量
	 */
	public ByteBufferChannel(int capacity) {
		init(capacity);
	}

	/**
	 * 构造函数
	 */
	public ByteBufferChannel() {
		init(1024);
	}

	/**
	 * 初始化函数
	 * @param capacity 分配的容量
	 */
	private void init(int capacity){
		lock = new ReentrantLock();
		this.byteBuffer = newByteBuffer(capacity);
		byteBuffer.limit(0);
		resetAddress();
		this.size = 0;
	}

	/**
	 * 构造一个ByteBuffer
	 * @param capacity 分配的容量
	 * @return ByteBuffer 对象
	 */
	private ByteBuffer newByteBuffer(int capacity){
		try {

			ByteBuffer instance = TByteBuffer.allocateDirect(capacity);
			address.set(TByteBuffer.getAddress(instance));

			return instance;

		}catch(Exception e){
			Logger.error("Create ByteBufferChannel error. ", e);
			return null;
		}
	}

	/**
	 * 是否已经释放
	 * @return true 已释放, false: 未释放
	 */
	public boolean isReleased(){
		if(address.get() == 0){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 测试是否被释放
	 */
	private void checkRelease(){
		if(isReleased()){
			throw new MemoryReleasedException("ByteBufferChannel is released.");
		}
	}

	/**
	 * 立刻释放内存
	 */
	public synchronized void release(){
		while(lock.isLocked()){
			TEnv.sleep(1);
		}

		lock.lock();
		try {
			if (address.get() != 0) {
				TByteBuffer.release(byteBuffer);
				address.set(0);
			}
		}finally{
			lock.unlock();
		}
	}

	private static class Deallocator implements Runnable {
		private long address;
		private int capacity;

		private Deallocator(long address, int capacity) {
			this.address = address;
			this.capacity = capacity;
		}

		public void setAddress(long address){
			this.address = address;
		}

		public void run() {

			if (this.address == 0) {
				return;
			}

			TUnsafe.getUnsafe().freeMemory(address);
			address = 0;
		}
	}

	/**
	 * 重新设置当前内存地址
	 */
	private void resetAddress(){
		checkRelease();

		lock.lock();
		try {
			this.address.set(TByteBuffer.getAddress(byteBuffer));
		}catch (ReflectiveOperationException e){
			Logger.error("ByteBufferChannel resetAddress() Error: ", e);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 当前数组空闲的大小
	 * @return 当前数组空闲的大小. -1: 已释放
	 */
	public int available(){
		if(isReleased()){
			return -1;
		}

		lock.lock();
		try {
			return byteBuffer.capacity() - size;
		}finally {
			lock.unlock();
		}
	}

	/**
	 * 返回当前分配的容量
	 * @return 当前分配的容量. -1: 已释放
	 */
	public int capacity(){
		if(isReleased()){
			return -1;
		}

		lock.lock();
		try {
			return byteBuffer.capacity();
		}finally {
			lock.unlock();
		}
	}

	/**
	 * 当前数据大小
	 * @return 数据大小 . -1: 已释放
	 */
	public int size(){
		if(isReleased()){
			return -1;
		}

		return size;
	}

	/**
	 * 获取缓冲区有效字节数组的一个拷贝
	 *        修改这个数组将不会影响当前对象
	 *        返回 0 到 size 的有效数据
	 *        从堆外复制到堆内
	 * @return 缓冲区有效字节数组. null: 已释放
	 */
	public byte[] array(){
		checkRelease();

		if(size()==0){
			return new byte[]{};
		}


		lock.lock();
		try {
			byte[] temp = new byte[size];
			get(temp, 0, size);
			return temp;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 清空通道
	 */
	public void clear(){
		if(isReleased()){
			return;
		}

		lock.lock();
		try{
			byteBuffer.limit(0);
			size = 0;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 从某一个偏移量位置开始收缩数据
	 * @param shrinkPosition      收缩的偏移量位置
	 * @param shrinkSize  收缩的数据大小, 大于0: 向尾部收缩, 小于0: 向头部收缩
	 * @return true: 成功, false: 失败
	 */
	public boolean shrink(int shrinkPosition, int shrinkSize){

		if(isReleased()){
			return false;
		}

		if(size()==0){
			return true;
		}

		if(shrinkSize==0){
			return true;
		}

		if(shrinkPosition < 0){
			return false;
		}

		if(shrinkSize < 0 && shrinkPosition + shrinkSize < 0){
			shrinkSize = shrinkPosition * -1;
		}

		if(shrinkSize > 0 && shrinkPosition + shrinkSize > size()){
			shrinkSize = size() - shrinkPosition;
		}

		if(Math.abs(shrinkSize) > size){
			return true;
		}

		lock.lock();
		try{
			int position = byteBuffer.position();
			byteBuffer.position(shrinkPosition);
			if(shrinkSize > 0){
				byteBuffer.position(shrinkPosition + shrinkSize);
			}
			if (TByteBuffer.moveData(byteBuffer, Math.abs(shrinkSize)*-1)) {
				if(position > shrinkPosition){
					position = position + shrinkPosition;
				}
				byteBuffer.position(position);
				size = size - Math.abs(shrinkSize);
				return true;
			}else{
				//收缩失败了,重置原 position 的位置
				byteBuffer.position(position);
				return false;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 收缩通道内的数据
	 *
	 * @param shrinkSize 收缩的偏移量: 大于0: 从头部向尾部收缩数据, 小于0: 从尾部向头部收缩数据
	 * @return true: 成功, false: 失败
	 */
	public boolean shrink(int shrinkSize){

		lock.lock();

		try{
			if(shrinkSize==0){
				return true;
			}else if(shrinkSize > 0)
				return shrink(0, shrinkSize);
			else
				return shrink(size, shrinkSize);
		} finally {
			lock.unlock();
		}
	}


	/**
	 * 获取某个位置的 byte 数据
	 *     该操作不会导致通道内的数据发生变化
	 * @param position 位置
	 * @return byte 数据
	 */
	public byte get(int position) throws IndexOutOfBoundsException {
		checkRelease();

		if(size()==0){
			throw new IndexOutOfBoundsException();
		}

		lock.lock();
		try{
			if(position >= 0 && position <= size) {
				byte result = unsafe.getByte(address.get() + position);
				return result;
			} else {
				throw new IndexOutOfBoundsException();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 获取某个位置的 byte 数据数组
	 *     该操作不会导致通道内的数据发生变化
	 * @param dst     目标数组
	 * @param position  位置
	 * @param length  长度
	 * @return 获取数据的长度
	 */
	public int get(byte[] dst, int position, int length) throws IndexOutOfBoundsException {
		checkRelease();

		if(size()==0){
			return 0;
		}

		lock.lock();
		try {
			int availableCount = size() - position;

			if(position >= 0 && availableCount >= 0) {

				if(availableCount == 0){
					return 0;
				}

				int arrSize = availableCount;

				if(length < availableCount){
					arrSize = length;
				}

				unsafe.copyMemory(null, address.get() + position, dst, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);

				return arrSize;

			} else {
				throw new IndexOutOfBoundsException();
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 获取某个偏移量位置的 byte 数据数组
	 *     该操作不会导致通道内的数据发生变化
	 * @param dst     目标数组
	 * @return 获取数据的长度
	 */
	public int get(byte[] dst){
		checkRelease();
		return get(dst, 0, dst.length);
	}

	/**
	 * 获取缓冲区
	 *     返回 0 到 size 的有效数据
	 *	   为了保证数据一致性, 这里会加锁
	 *	   在调用getByteBuffer()方法后,跨线程的读写操作都会被阻塞.
	 *	   但在调用getByteBuffer()方法后,同一线程内的所有读写操作是可以操作,
	 *	   为保证数据一致性,除非特殊需要,否则在编码时应当被严格禁止.
	 *	   在调用getByteBuffer()方法后,所以必须配合 compact() 方法使用,
	 *	   已保证对 byteBuffer 的所有读写操作都在 ByteBufferChannel上生效.
	 * @return ByteBuffer 对象
	 */
	public ByteBuffer getByteBuffer(){
		checkRelease();

		//这里上锁,在compact()方法解锁
		lock.lock();
		return byteBuffer;
	}

	/**
	 * 收缩通道
	 *      将通过 getByteBuffer() 方法获得 ByteBuffer 对象的操作同步到 ByteBufferChannel
	 * 		如果之前最后一次通过 getByteBuffer() 方法获得过 ByteBuffer,则使用这个 ByteBuffer 来收缩通道
	 *      将 (position 到 limit) 之间的数据 移动到 (0  到 limit - position) 其他情形将不做任何操作
	 *		所以 必须 getByteBuffer() 和 compact() 成对操作
	 * @return 是否compact成功,true:成功, false:失败
	 */
	public boolean compact(){
		if(isReleased()){
			return false;
		}

		if(!lock.isLocked()) {
			lock.lock();
		}

		if(size()==0){
			if(lock.isLocked()){
				lock.unlock();
			}
			return true;
		}

		try{

			if(byteBuffer.position() == 0){
				return true;
			}

			int position = byteBuffer.position();
			int limit = byteBuffer.limit();
			boolean result = false;
			if(TByteBuffer.moveData(byteBuffer, position*-1)) {
				byteBuffer.position(0);
				size = limit - position;
				byteBuffer.limit(size);

				result = true;
			}
			return result;

		} finally {
			if(lock.isLocked()) {
				lock.unlock();
			}
		}
	}

	/**
	 * 等待期望的数据长度
	 * @param length  期望的数据长度
	 * @param timeout 超时时间,单位: 秒
	 * @return true: 具备期望长度的数据, false: 等待数据超时
	 */
	public boolean waitData(int length,int timeout){
		while(timeout > 0){
			if(isReleased()){
				throw new MemoryReleasedException("ByteBufferChannel is released.");
			}

			if(size() >= length){
				return true;
			}
			timeout -- ;
			TEnv.sleep(1);
		}
		return false;
	}


	/**
	 * 等待收到期望的数据
	 * @param mark  期望出现的数据
	 * @param timeout 超时时间,单位: 秒
	 * @return true: 具备期望长度的数据, false: 等待数据超时
	 */
	public boolean waitData(byte[] mark, int timeout){
		while(timeout > 0){
			if(isReleased()){
				throw new MemoryReleasedException("ByteBufferChannel is released.");
			}

			if(indexOf(mark) != -1){
				return true;
			}
			timeout -- ;
			TEnv.sleep(1);
		}
		return false;
	}

	/**
	 * 重新分配内存空间的大小
	 * @param newSize  重新分配的空间大小
	 * @return true:成功, false:失败
	 */
	public boolean reallocate(int newSize){
		checkRelease();
		lock.lock();
		try{
			if (TByteBuffer.reallocate(byteBuffer, newSize)) {
				resetAddress();
				return true;
			}else{
				return false;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 缓冲区某个位置写入数据
	 * @param writePosition 缓冲区中的位置
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 写入的数据大小
	 */
	public int write(int writePosition, ByteBuffer src) {
		checkRelease();

		if(src.remaining() == 0){
			return 0;
		}

		if (src == null) {
			return -1;
		}

		lock.lock();
		try {

			int writeSize = src.limit() - src.position();

			if (writeSize > 0) {
				//是否扩容
				if (available() < writeSize) {
					int newSize = byteBuffer.capacity() + writeSize;
					reallocate(newSize);
				}


				int position = byteBuffer.position();

				byteBuffer.position(writePosition);

				if(TByteBuffer.moveData(byteBuffer, writeSize)){

					size = size + writeSize;
					byteBuffer.limit(size);
					byteBuffer.position(writePosition);
					byteBuffer.put(src);

					if (position > writePosition) {
						position = position + writeSize;
					}

					byteBuffer.position(position);
				} else {
					throw new RuntimeException("move data failed");
				}
			}

			return writeSize;

		} finally {
			lock.unlock();
		}
	}

	/**
	 * 缓冲区头部写入
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 写入的数据大小
	 */
	public int writeEnd(ByteBuffer src) {
		checkRelease();

		lock.lock();
		try {
			return write(size(), src);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 缓冲区尾部写入
	 * @param src 需要写入的缓冲区 ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public int writeHead(ByteBuffer src) {
		checkRelease();

		lock.lock();
		try {
			return write(0, src);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 从缓冲区头部读取数据
	 * @param dst 需要读入数据的缓冲区ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public int readHead(ByteBuffer dst) {
		checkRelease();

		lock.lock();
		try {
			return read(0, dst);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 从缓冲区尾部读取数据
	 * @param dst 需要读入数据的缓冲区ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public int readEnd(ByteBuffer dst) {
		checkRelease();

		lock.lock();
		try {
			return read( size-dst.limit(), dst );
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 从缓冲区某个位置开始读取数据
	 * @param readPosition 缓冲区中的位置
	 * @param dst 需要读入数据的缓冲区ByteBuffer 对象
	 * @return 读出的数据大小
	 */
	public int read(int readPosition, ByteBuffer dst) {
		checkRelease();

		if(dst.remaining() == 0){
			return 0;
		}

		if(dst==null){
			return -1;
		}

		lock.lock();
		try {

			int readSize = 0;

			//确定读取大小
			if (dst.remaining() > size - readPosition) {
				readSize = size - readPosition;
			} else {
				readSize = dst.remaining();
			}

			if (readSize != 0) {
				int position = byteBuffer.position();
				byteBuffer.position(readPosition);

				for (int i = 0; i < readSize; i++) {
					dst.put(byteBuffer.get());
				}

				if (TByteBuffer.moveData(byteBuffer, (readSize*-1))) {
					size = size - readSize;
					byteBuffer.limit(size);

					if(position > readPosition){
						position = position + (readSize*-1);
					}

					byteBuffer.position(position);
				} else {
					dst.reset();
				}
			}

			dst.flip();
			return readSize;

		} finally {
			lock.unlock();
		}

	}

	/**
	 * 查找特定 byte 标识的位置
	 *     byte 标识数组第一个字节的索引位置
	 * @param mark byte 标识数组
	 * @return 第一个字节的索引位置
	 */
	public int indexOf(byte[] mark){
		checkRelease();

		lock.lock();
		try {
			if(size() == 0){
				return -1;
			}

			int index = -1;
			byte[] tmp = new byte[mark.length];
			for(int position = 0; position <= size() - mark.length; position++){
				get(tmp, position, tmp.length);
				if(Arrays.equals(mark, tmp)){
					index = position;
					break;
				}
			}

			return index;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 读取一行
	 * @return 字符串
	 */
	public String readLine() {
		checkRelease();

		if(size() == 0){
			return null;
		}

		if (size() == 0) {
			return null;
		}

		String lineStr = "";
		int index = indexOf("\n".getBytes());

		if (index >= 0) {

			ByteBuffer lineBuffer = TByteBuffer.allocateDirect(index + 1);

			int readSize = readHead(lineBuffer);

			if (readSize == index + 1) {
				lineStr = TByteBuffer.toString(lineBuffer);
			}
			TByteBuffer.release(lineBuffer);
		}

		if(size()>0 && lineStr.isEmpty()){
			ByteBuffer lineBuffer = TByteBuffer.allocateDirect(size());
			if(readHead(lineBuffer) > 0) {
				lineStr = TByteBuffer.toString(lineBuffer);
			}
			TByteBuffer.release(lineBuffer);
		}

		return lineStr.isEmpty() ? null : lineStr;
	}


	/**
	 * 读取一段,使用 byte数组 分割
	 * 		返回的 byte数组中不包含分割 byte 数组的内容
	 * @param splitByte 分割字节数组
	 * @return 字节数组
	 */
	public ByteBuffer readWithSplit(byte[] splitByte) {
		checkRelease();

		if(size() == 0){
			return emptyByteBuffer;
		}

		int index = indexOf(splitByte);

		if (size() == 0) {
			return emptyByteBuffer;
		}

		if (index == 0) {
			this.getByteBuffer().position(splitByte.length);
			compact();
			index = indexOf(splitByte);
		}

		if (index == -1) {
			index = size();
		}

		ByteBuffer resultBuffer = ByteBuffer.allocateDirect(index);
		int readSize = readHead(resultBuffer);
		TByteBuffer.release(resultBuffer);

		//跳过分割符
		shrink(splitByte.length);

		return resultBuffer;

	}

	/**
	 * 保存到文件
	 * @param filePath 文件路径
	 * @param length 需要保存的长度
	 * @throws IOException Io 异常
	 */
	public void saveToFile(String filePath, long length) throws IOException{
		checkRelease();

		if(size() == 0){
			return;
		}

		int bufferSize = 1024 * 1024;

		if (length < bufferSize) {
			bufferSize = Long.valueOf(length).intValue();
		}

		new File(TFile.getFileDirectory(filePath)).mkdirs();

		RandomAccessFile randomAccessFile = null;
		File file = new File(filePath);
		byte[] buffer = new byte[bufferSize];
		try {
			randomAccessFile = new RandomAccessFile(file, "rwd");
			//追加形式
			randomAccessFile.seek(randomAccessFile.length());

			int loadSize = bufferSize;
			while (length > 0) {
				loadSize = length > bufferSize ? bufferSize : new Long(length).intValue();
				get(buffer, 0, loadSize);
				randomAccessFile.write(buffer, 0, loadSize);

				length = length - loadSize;
			}

			compact();
		} catch (IOException e) {
			throw e;
		} finally {
			randomAccessFile.close();
		}

	}

	@Override
	public String toString(){
		return "{size="+size+", capacity="+capacity()+", released="+(address.get()==0)+"}";
	}

	/**
	 * 获取内容
	 * @return 内容字符串
	 */
	public String content(){
		return new String(array());
	}
}

