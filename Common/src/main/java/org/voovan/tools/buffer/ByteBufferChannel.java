package org.voovan.tools.buffer;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TProperties;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.exception.LargerThanMaxSizeException;
import org.voovan.tools.exception.MemoryReleasedException;
import org.voovan.tools.log.Logger;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ByteBuffer双向通道
 *      默认: 非线程安全模式
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ByteBufferChannel {
    private static int BYTEBUFFERCHANNEL_MAX_SIZE = TProperties.getInt("framework", "ByteBufferChannelMaxSize", 1024*1024*2);
    private volatile long address = 0;
    private Unsafe unsafe = TUnsafe.getUnsafe();
    private ByteBuffer byteBuffer;
    private volatile int capacity;
    private volatile int size;
    private ReentrantLock lock;
    private AtomicBoolean borrowed = new AtomicBoolean(false);

    private int maxSize = BYTEBUFFERCHANNEL_MAX_SIZE;

    private boolean isThreadSafe = false;

    /**
     * 构造函数
     * @param capacity 初始分配的容量, 会自动扩容 maxSize 的大小, 如果容量不够抛出异常
     */
    public ByteBufferChannel(int capacity) {
        init(capacity);
    }

    /**
     * 构造函数
     * @param capacity 初始分配的容量, 会自动扩容 maxSize 的大小, 如果容量不够抛出异常
     * @param maxSize 通道地最大容量
     */
    public ByteBufferChannel(int capacity, Integer maxSize) {
        init(capacity);
        this.maxSize = maxSize;
    }

    /**
     * 构造函数
     * @param byteBuffer 初始分配的容量, 会自动扩容 maxSize 的大小, 如果容量不够抛出异常
     */
    public ByteBufferChannel(ByteBuffer byteBuffer) {
        init(byteBuffer);
    }


    /**
     * 构造函数
     */
    public ByteBufferChannel() {
        init(TByteBuffer.DEFAULT_BYTE_BUFFER_SIZE);
    }

    /**
     * 更换内部的 ByteBuffer 为新的 ByteBuffer
     * @return 旧的 ByteBuffer
     */
    public ByteBuffer newBuffer() {
        ByteBuffer oldByteBuffer = this.byteBuffer;
        init(capacity);
        return oldByteBuffer;
    }

    /**
     * 初始化函数
     * @param capacity 初始分配的容量, 会自动扩容到 maxSize 的大小, 如果容量不够抛出异常
     */
    private void init(int capacity){
        lock = new ReentrantLock(true);
        this.byteBuffer = newByteBuffer(capacity);
        byteBuffer.limit(0);
        resetAddress();
        this.capacity = capacity;
        this.size = 0;
        this.maxSize = maxSize < capacity ? capacity : maxSize;
    }

    /**
     * 初始化函数
     * @param byteBuffer 初始化用的 ByteBuffer
     */
    public void init(ByteBuffer byteBuffer){
        lock = new ReentrantLock(true);
        this.byteBuffer = byteBuffer;
        resetAddress();
        this.size = byteBuffer.remaining();
    }

    /**
     * 构造一个ByteBuffer
     * @param capacity 分配的容量
     * @return ByteBuffer 对象
     */
    private ByteBuffer newByteBuffer(int capacity){
        try {

            ByteBuffer instance = TByteBuffer.allocateDirect(capacity);
            address = TByteBuffer.getAddress(instance);

            return instance;

        }catch(Exception e){
            Logger.error("Create ByteBufferChannel error. ", e);
            return null;
        }
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * 获取是否是线程安全的
     * @return true: 线程安全, false: 非线程安全
     */
    public boolean isThreadSafe() {
        return isThreadSafe;
    }

    /**
     * 设置是否启用线程安全模式
     * @param threadSafe true: 启用线程安全, false: 不启用线程安全
     */
    public void setThreadSafe(boolean threadSafe) {
        isThreadSafe = threadSafe;
    }

    public void lock(){
        if(isThreadSafe) {
            lock.lock();
        }
    }

    public void unlock(){
        if(isThreadSafe) {
            lock.unlock();
        }
    }

    /**
     * 缓冲通道是否已满
     * @return true: 通道已满, false: 通道未满
     */
    public boolean isFull(){
        return maxSize <= size;
    }

    /**
     * 缓冲通道是否空
     * @return true: 通道空, false: 通道有数据
     */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * 是否已经释放
     * @return true 已释放, false: 未释放
     */
    public boolean isReleased(){
        if(address == 0 || byteBuffer == null){
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
    public void release(){
        if(byteBuffer==null){
            return;
        }

        lock();
        try {
            if (address != 0) {
                TByteBuffer.release(byteBuffer);
                address = 0;
                byteBuffer = null;
                size = -1;
            }
        } finally {
            while(lock.isLocked() && lock.isHeldByCurrentThread()) {
                unlock();
            }
        }
    }

    /**
     * 重新设置当前内存地址
     */
    private void resetAddress(){
        lock();
        try {
            this.address = TByteBuffer.getAddress(byteBuffer);
        }catch (ReflectiveOperationException e){
            Logger.error("ByteBufferChannel resetAddress() Error: ", e);
        } finally {
            unlock();
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

        lock();
        try {
            return byteBuffer.capacity() - size;
        }finally {
            unlock();
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

        lock();
        try {
            return byteBuffer.capacity();
        }finally {
            unlock();
        }
    }

    /**
     * 当前数据大小
     * @return 数据大小 . -1: 已释放
     */
    public int size(){
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
        if(size()==0){
            return new byte[]{};
        }

        lock();

        try {
            checkRelease();

            byte[] temp = new byte[size];
            get(temp, 0, size);
            return temp;
        } finally {
            unlock();
        }
    }

    /**
     * 清空通道
     */
    public void clear() {
        if (isReleased()) {
            return;
        }

        lock();
        try {
            if (byteBuffer != null) {
                byteBuffer.position(0);
                byteBuffer.limit(0);
                size = 0;
            }
        } finally{
            unlock();
        }
    }

    /**
     * 从某一个偏移量位置开始收缩数据
     * @param shrinkPosition      收缩的偏移量位置
     * @param shrinkSize  收缩的数据大小, 大于0: 向尾部收缩, 小于0: 向头部收缩
     * @return true: 成功, false: 失败
     */
    public boolean shrink(int shrinkPosition, int shrinkSize){
        lock();
        try{
            checkRelease();

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

            int position = byteBuffer.position();
            byteBuffer.position(shrinkPosition);
            if(shrinkSize > 0){
                byteBuffer.position(shrinkPosition + shrinkSize);
            }
            if (TByteBuffer.move(byteBuffer, Math.abs(shrinkSize)*-1)) {
                if(position > shrinkPosition){
                    position = position + shrinkPosition;
                }

                if(position < byteBuffer.limit()) {
                    byteBuffer.position(position);
                }

                size = size - Math.abs(shrinkSize);
                return true;
            }else{
                //收缩失败了,重置原 position 的位置
                byteBuffer.position(position);
                return false;
            }
        } finally {
            unlock();
        }
    }

    /**
     * 收缩通道内的数据
     *
     * @param shrinkSize 收缩的偏移量: 大于0: 从头部向尾部收缩数据, 小于0: 从尾部向头部收缩数据
     * @return true: 成功, false: 失败
     */
    public boolean shrink(int shrinkSize){

        lock();

        try{
            if(shrinkSize==0){
                return true;
            }else if(shrinkSize > 0)
                return shrink(0, shrinkSize);
            else
                return shrink(size, shrinkSize);
        } finally {
            unlock();
        }
    }


    /**
     * 获取 bytebuffer 的 hashcode
     * @param size slice 的数据大小
     * @return bytebuffer 的 hashcode
     */
    public ByteBuffer slice(int size){

        lock();
        int oldLimit = byteBuffer.limit();
        try {
            checkRelease();

            byteBuffer.limit(byteBuffer.position()+size);
            return byteBuffer.slice();
        } finally {
            byteBuffer.limit(oldLimit);
            unlock();
        }
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
    public ByteBuffer getByteBuffer() {
        //这里上锁,在compact()方法解锁
        lock();
        try {
            checkRelease();

            borrowed.compareAndSet(false, true);
            return byteBuffer;
        } catch (Exception e) {
            return null;
        }
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
            if(lock.isHeldByCurrentThread() && borrowed.compareAndSet(true, false)) {
                unlock();
            }
            return false;
        }

        if(size()==0 && !byteBuffer.hasRemaining()){
            if(lock.isHeldByCurrentThread() && borrowed.compareAndSet(true, false)){
                unlock();
            }
            return true;
        }

        try{

            if(byteBuffer.position() == 0){
                this.size = byteBuffer.limit();
                return true;
            }

            int position = byteBuffer.position();
            int limit = byteBuffer.limit();
            boolean result = false;
            if(TByteBuffer.move(byteBuffer, position*-1)) {
                byteBuffer.position(0);
                size = limit - position;
                byteBuffer.limit(size);

                result = true;
            }
            return result;

        } finally {
            if(borrowed.compareAndSet(true, false)) {
                unlock();
            }
        }
    }


    /**
     * 获取某个位置的 byte 数据
     *     该操作不会导致通道内的数据发生变化
     * @param position 位置
     * @return byte 数据
     */
    public byte get(int position) throws IndexOutOfBoundsException {
        lock();

        try{
            checkRelease();

            if(size()==0){
                throw new IndexOutOfBoundsException();
            }

            if(position >= 0 && position <= size) {
                byte result = unsafe.getByte(address + position);
                return result;
            } else {
                checkRelease();
                throw new IndexOutOfBoundsException();
            }
        } finally {
            unlock();
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
        lock();

        try {
            checkRelease();

            if(size()==0){
                return 0;
            }

            int availableCount = size() - position;

            if(position >= 0 && availableCount >= 0) {

                if(availableCount == 0){
                    return 0;
                }

                int dataSize = availableCount;

                if(length < availableCount){
                    dataSize = length;
                }

                unsafe.copyMemory(null, address + position, dst, Unsafe.ARRAY_BYTE_BASE_OFFSET, length);

                return dataSize;

            } else {
                checkRelease();
                throw new IndexOutOfBoundsException();
            }
        } finally {
            unlock();
        }
    }

    /**
     * 获取某个偏移量位置的 byte 数据数组
     *     该操作不会导致通道内的数据发生变化
     * @param dst     目标数组
     * @return 获取数据的长度
     */
    public int get(byte[] dst){
        return get(dst, 0, dst.length);
    }

    /**
     * 读取数据到 srcByteBuffer
     * @param srcByteBuffer 接收数据的 ByteBuffer 对象
     * @return 获取数据的长度
     */
    public int get(ByteBuffer srcByteBuffer){
        lock();

        try {
            checkRelease();
            int length = srcByteBuffer.remaining();

            if(size()==0){
                return 0;
            }

            int availableCount = size();

            if(availableCount >= 0) {

                if(availableCount == 0){
                    return 0;
                }

                int dataSize = availableCount;

                if(length < availableCount){
                    dataSize = length;
                }

                for(int i=0;i<dataSize;i++) {
                    srcByteBuffer.put(i, get(i));
                }

                return dataSize;

            } else {
                checkRelease();
                throw new IndexOutOfBoundsException();
            }
        } finally {
            unlock();
        }

    }

    /**
     * 缓冲区某个位置写入数据
     * @param writePosition 缓冲区中的位置
     * @param srcBytes 源字节数组
     * @param offset 字节数组便宜
     * @param length 写入数据长度
     * @return 写入的数据大小
     */
    private int write0(int writePosition, byte[] srcBytes, int offset, int length) {
//        lock();
//
//        try {
//            checkRelease();
//
            if(length == 0){
                return 0;
            }

            if (srcBytes == null) {
                return -1;
            }

            int writeSize = length;

            if (writeSize > 0) {
                //是否扩容
                if (available() < writeSize) {
                    int newSize = byteBuffer.capacity() + writeSize;
                    reallocate(newSize);
                }

                int position = byteBuffer.position();
                byteBuffer.position(writePosition);

                if(TByteBuffer.move(byteBuffer, writeSize)){

                    size = size + writeSize;
                    byteBuffer.limit(size);
                    byteBuffer.position(writePosition);


                    unsafe.copyMemory(srcBytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + offset,
                            null, address + writePosition,
                            writeSize);
                    byteBuffer.position(byteBuffer.position() + writeSize);


                    if (position > writePosition) {
                        position = position + writeSize;
                    }

                    byteBuffer.position(position);
                } else {
                    checkRelease();
                    throw new RuntimeException("move data failed");
                }
            }

            return writeSize;

//        } finally {
//            unlock();
//        }
    }

    /**
     * 缓冲区某个位置写入数据
     * @param writePosition 缓冲区中的位置
     * @param srcBytes 源字节数组
     * @param offset 字节数组便宜
     * @param length 写入数据长度
     * @return 写入的数据大小
     */
    public int write(int writePosition, byte[] srcBytes, int offset, int length) {
        lock();

        try {
            checkRelease();

            return write0(writePosition, srcBytes, offset, length);
        } finally {
            unlock();
        }
    }

    /**
     * 缓冲区头部写入
     * @param srcBytes 源字节数组
     * @param offset 字节数组便宜
     * @param length 写入数据长度
     * @return 写入的数据大小
     */
    public int writeEnd(byte[] srcBytes, int offset, int length) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return write0(size(), srcBytes, offset, length);
        } finally {
            unlock();
        }
    }

    /**
     * 缓冲区尾部写入
     * @param srcBytes 源字节数组
     * @param offset 字节数组便宜
     * @param length 写入数据长度
     * @return 读出的数据大小
     */
    public int writeHead(byte[] srcBytes, int offset, int length) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return write0(0, srcBytes, offset, length);
        } finally {
            unlock();
        }
    }

    /**
     * 缓冲区某个位置写入数据
     * @param writePosition 缓冲区中的位置
     * @param src 需要写入的缓冲区 ByteBuffer 对象
     * @return 写入的数据大小
     */
    private int write0(int writePosition, ByteBuffer src) {
        lock();

        try {
            checkRelease();

            if(src.remaining() == 0){
                return 0;
            }

            if (src == null) {
                return -1;
            }

            int writeSize = src.limit() - src.position();

            if (writeSize > 0) {
                //是否扩容
                if (available() < writeSize) {
                    int newSize = byteBuffer.capacity() + writeSize;
                    reallocate(newSize);
                }

                int position = byteBuffer.position();
                byteBuffer.position(writePosition);

                if(TByteBuffer.move(byteBuffer, writeSize)){

                    size = size + writeSize;
                    byteBuffer.limit(size);
                    byteBuffer.position(writePosition);

                    if(!src.isDirect()) {
                        byte[] srcBytes = src.array();
                        unsafe.copyMemory(srcBytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + src.position(),
                                null, address + writePosition,
                                writeSize);
                        src.position(src.position() + writeSize);
                        byteBuffer.position(byteBuffer.position() + writeSize);
                    } else {
                        try {
                            long srcAddress = TByteBuffer.getAddress(src);
                            unsafe.copyMemory(srcAddress + src.position(),
                                    address + writePosition,
                                    writeSize);
                            src.position(src.position() + writeSize);
                            byteBuffer.position(byteBuffer.position() + writeSize);
                        } catch (ReflectiveOperationException e) {
                            byteBuffer.put(src);
                            e.printStackTrace();
                        }
                    }

                    if (position > writePosition) {
                        position = position + writeSize;
                    }

                    byteBuffer.position(position);
                } else {
                    checkRelease();
                    throw new RuntimeException("move data failed");
                }
            }

            return writeSize;

        } finally {
            unlock();
        }
    }

    /**
     * 缓冲区某个位置写入数据
     * @param writePosition 缓冲区中的位置
     * @param src 需要写入的缓冲区 ByteBuffer 对象
     * @return 写入的数据大小
     */
    public int write(int writePosition, ByteBuffer src) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return write0(writePosition, src);
        } finally {
            unlock();

        }
    }

    /**
     * 缓冲区头部写入
     * @param src 需要写入的缓冲区 ByteBuffer 对象
     * @return 写入的数据大小
     */
    public int writeEnd(ByteBuffer src) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return write0(size(), src);
        } finally {
            unlock();
        }
    }

    /**
     * 缓冲区尾部写入
     * @param src 需要写入的缓冲区 ByteBuffer 对象
     * @return 读出的数据大小
     */
    public int writeHead(ByteBuffer src) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return write(0, src);
        } finally {
            unlock();
        }
    }


    /**
     * 从缓冲区某个位置开始读取数据
     * @param readPosition 缓冲区中的位置
     * @param dst 需要读入数据的缓冲区ByteBuffer 对象
     * @return 读出的数据大小
     */
    private int read0(int readPosition, ByteBuffer dst) {
        lock();

        try {
            checkRelease();

            if(dst.remaining() == 0){
                return 0;
            }

            if(dst==null){
                return -1;
            }

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

                int dstRemain = dst.remaining();
                int oldLimit = byteBuffer.limit();
                if(dstRemain<byteBuffer.remaining()) {
                    byteBuffer.limit(dstRemain);
                }
//                dst.put(byteBuffer);

                if(!dst.isDirect()) {
                    byte[] dstBytes = dst.array();
                    unsafe.copyMemory(null, address + readPosition,
                                        dstBytes, Unsafe.ARRAY_BYTE_BASE_OFFSET + dst.position(),
                                        readSize);
                    dst.position(dst.position()+readSize);
                    byteBuffer.position(byteBuffer.position() + readSize);
                } else {
                    try {
                        long dstAddress = TByteBuffer.getAddress(dst);
                        unsafe.copyMemory(address + readPosition, dstAddress + dst.position(), readSize);
                        dst.position(dst.position()+readSize);
                        byteBuffer.position(byteBuffer.position() + readSize);
                    } catch (ReflectiveOperationException e) {
                        byteBuffer.put(dst);
                        e.printStackTrace();
                    }
                }

                byteBuffer.limit(oldLimit);

                if (TByteBuffer.move(byteBuffer, (readSize*-1))) {
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
            unlock();
        }
    }

    /**
     * 从缓冲区某个位置开始读取数据
     * @param readPosition 缓冲区中的位置
     * @param dst 需要读入数据的缓冲区ByteBuffer 对象
     * @return 读出的数据大小
     */
    public int read(int readPosition, ByteBuffer dst) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return read0(readPosition, dst);
        } finally {
            unlock();
        }
    }

    /**
     * 从缓冲区头部读取数据
     * @param dst 需要读入数据的缓冲区ByteBuffer 对象
     * @return 读出的数据大小
     */
    public int readHead(ByteBuffer dst) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return read0(0, dst);
        } finally {
            unlock();
        }
    }

    /**
     * 从缓冲区尾部读取数据
     * @param dst 需要读入数据的缓冲区ByteBuffer 对象
     * @return 读出的数据大小
     */
    public int readEnd(ByteBuffer dst) {
        //这里加锁的作用是防止 size 发生变化
        lock();

        try {
            checkRelease();

            return read0( size()-dst.limit(), dst );
        } finally {
            unlock();
        }
    }

    /**
     * 等待期望的数据长度
     * @param length  期望的数据长度
     * @param timeout 超时时间,单位: 毫秒
     * @param supplier 每次等待数据所做的操作
     * @return true: 具备期望长度的数据, false: 等待数据超时
     */
    public boolean waitData(int length,int timeout, Runnable supplier){
        return TEnv.wait(timeout, ()->{
            checkRelease();

            if(size() >= length){
                return false;
            } else {
                supplier.run();
                return size() < length;
            }
        });
    }


    /**
     * 从头部开始判断是否收到期望的数据
     * @param mark  期望出现的数据
     * @param timeout 超时时间,单位: 毫秒
     * @param supplier 每次等待数据所做的操作
     * @return true: 具备期望长度的数据, false: 等待数据超时
     */
    public boolean waitData(byte[] mark, int timeout, Runnable supplier){

        return TEnv.wait(timeout, ()->{
            checkRelease();
            if(indexOf(mark) != -1) {
                return false;
            } else {
                supplier.run();
                return indexOf(mark) == -1;
            }
        });

    }

    /**
     * 重新分配内存空间的大小
     * @param newSize  重新分配的空间大小
     * @return true:成功, false:失败
     * @throws LargerThanMaxSizeException 通道容量不足的一场
     */
    public boolean reallocate(int newSize) throws LargerThanMaxSizeException {
        lock();

        try{
            checkRelease();

            //检查分配内存是否超过限额
            if(maxSize < newSize){
                throw new LargerThanMaxSizeException("Max size: " + maxSize + ", expect size: " + newSize);
            }

            if (TByteBuffer.reallocate(byteBuffer, newSize)) {
                resetAddress();
                return true;
            }else{
                return false;
            }
        } finally {
            unlock();
        }
    }

    /**
     * 查找特定 byte 标识的位置
     *     byte 标识数组第一个字节的索引位置
     * @param mark byte 标识数组
     * @return 第一个字节的索引位置
     */
    public int indexOf(byte[] mark){
        lock();

        try {
            checkRelease();

            if(size() == 0){
                return -1;
            }

            return TByteBuffer.indexOf(byteBuffer, mark);
        } finally {
            unlock();
        }
    }

    public boolean startWith(byte[] mark){
        checkRelease();

        if(size() < mark.length){
            return false;
        }

        boolean result = true;

        for(int i=0;i<mark.length; i++){
            if(mark[i] != get(i)){
                result = false;
                break;
            }
        }

        return result;
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

        String lineStr = "";
        int index = indexOf("\n".getBytes());

        if (index >= 0) {
            ByteBuffer byteBuffer = getByteBuffer();

            try {
                if (byteBuffer == null) {
                    return null;
                }

                int limit = byteBuffer.limit();
                byteBuffer.limit(index + 1);
                lineStr = TByteBuffer.toString(byteBuffer);
                byteBuffer.limit(limit);
                byteBuffer.position(index + 1);
            } finally {
                compact();
            }
        }

        if(size()>0 && index==-1){
            ByteBuffer byteBuffer = getByteBuffer();

            try {
                lineStr = TByteBuffer.toString(byteBuffer);
                byteBuffer.position(byteBuffer.limit());
            } finally {
                compact();
            }
        }

        return lineStr.isEmpty() && index==-1 ? null : lineStr;
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
            return TByteBuffer.EMPTY_BYTE_BUFFER;
        }

        int index = indexOf(splitByte);

        if (index == 0) {
            try {
                this.getByteBuffer().position(splitByte.length);
            } finally {
                compact();
            }

            index = indexOf(splitByte);
        }

        if (index == -1) {
            index = size();
        }

        ByteBuffer resultBuffer = TByteBuffer.allocateDirect(index);
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

        TFile.mkdir(filePath);

        RandomAccessFile randomAccessFile = null;
        File file = new File(filePath);
        byte[] buffer = new byte[bufferSize];
        try {
            randomAccessFile = new RandomAccessFile(file, "rwd");
            //追加形式
            randomAccessFile.seek(randomAccessFile.length());

            int loadSize = bufferSize;
            ByteBuffer tempByteBuffer = ByteBuffer.wrap(buffer);
            while (length > 0) {
                loadSize = length > bufferSize ? bufferSize : Long.valueOf(length).intValue();

                tempByteBuffer.limit(loadSize);

                this.readHead(tempByteBuffer);

                randomAccessFile.write(buffer, 0, loadSize);

                length = length - loadSize;

                tempByteBuffer.clear();
            }

//			System.out.println(filePath);
        } catch (IOException e) {
            throw e;
        } finally {
            randomAccessFile.close();
        }

    }

    @Override
    public String toString(){
        return "{size="+size+", capacity="+capacity()+", released="+(address==0)+", maxSize=" + maxSize + "}";
    }

    /**
     * 获取内容
     * @return 内容字符串
     */
    public String content(){
        return new String(array());
    }
}
