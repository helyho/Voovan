package org.voovan.tools;

import org.voovan.tools.exception.MemoryReleasedException;
import org.voovan.tools.log.Logger;
import sun.misc.Unsafe;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

/**
 * 环形缓冲区
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RingDirectBuffer {
    private static Unsafe unsafe = TUnsafe.getUnsafe();
    private ByteBuffer byteBuffer;
    private long address = 0;
    private int readPositon = 0;
    private int writePositon = 0;
    private int capacity;

    /**
     * 使用默认容量构造一个环形缓冲区
     */
    public RingDirectBuffer(){
        this(TByteBuffer.DEFAULT_BYTE_BUFFER_SIZE);
    }

    /**
     * 使用指定容量构造一个环形缓冲区
     * @param capacity 分配的容量
     */
    public RingDirectBuffer(int capacity){
    	this(TByteBuffer.allocateDirect(capacity));
    }

    /**
     * 使用指定容量构造一个环形缓冲区
     * @param byteBuffer ByteBuffer 对象
     */
    public RingDirectBuffer(ByteBuffer byteBuffer){
        if(byteBuffer.hasArray()){
            throw new UnsupportedOperationException();
        }

        this.capacity = byteBuffer.capacity();
        this.byteBuffer = byteBuffer;
        try {
            this.address = TByteBuffer.getAddress(byteBuffer);
        } catch (ReflectiveOperationException e) {
            Logger.error("Get bytebuffer address error.");
        }
    }

    /**
     * 获得环形缓冲区的基地址
     * @return 环形缓冲区的基地址
     */
    public long getAddress() {
        return address;
    }

    /**
     * 获得读指针位置
     * @return 读指针位置
     */
    public int getReadPositon() {
        return readPositon;
    }

    /**
     * 获得写指针位置
     * @return 写指针位置
     */
    public int getWritePositon() {
        return writePositon;
    }

    /**
     * 获得容量
     * @return 容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 读指针跳过特定的偏移量
     * @param offset 偏移量
     */
    public boolean skip(int offset){
        checkRelease();

        if(remaining() < offset || offset < 0){
           return false;
        }

        readPositon = ( readPositon + offset ) % capacity;
        return true;
    }

    /**
     * 缓冲区空判断
     * @return true: 缓冲区无可用数据, false: 缓冲区有可用数据
     */
    public Boolean isEmpty() {
        return readPositon == writePositon;
    }

    /**
     * 缓冲区满判断
     * @return true: 缓冲区已满, false: 缓冲区未满
     */
    public Boolean isFull() {
        return (writePositon + 1) % capacity == readPositon;
    }

    /**
     * 清理缓冲区
     */
    public void clear() {
        checkRelease();

        this.readPositon = 0;
        this.writePositon = 0;
    }

    /**
     * 获得基于索引位置的数据
     * @param offset 偏移量
     * @return byte 数据
     */
    public byte get(int offset) {
        checkRelease();

        if(offset < remaining()) {
            int realOffset = (readPositon + offset) % capacity;
            return unsafe.getByte(address + realOffset);
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * 读取所有缓冲区的数据
     *      不影响读写位置
     * @param byteBuffer ByteBuffer 对象
     * @return 读取数据大小
     */
    public int get(ByteBuffer byteBuffer) {
        checkRelease();

        int size = byteBuffer.remaining();
        if(size > remaining()){
            size = remaining();
        }

        for(int i=0;i<size;i++){
            byteBuffer.put(get(i));
        }

        return size;
    }

    /**
     * 读取所有缓冲区的数据
     *      不影响读写位置
     * @param bytes 用于读取数据的 byte 数组
     * @param offset 偏移量
     * @param length 数据长度
     * @return 读取数据大小
     */
    public int get(byte[] bytes, int offset, int length) {
        checkRelease();

        if(length > remaining()){
            length = remaining();
        }

        for(int i=offset;i<offset+length;i++){
            bytes[i] = get(i);
        }

        return length;
    }


    /**
     * 读取所有缓冲区的数据
     *      不影响读写位置
     * @return 读取数据
     */
    public byte[] toArray() {
        byte[] bytes = new byte[remaining()];
        get(bytes, 0, bytes.length);
        return bytes;
    }

    /**
     * 写入一个 byte
     * @param b byte 数据
     */
    public void write(byte b) {
        checkRelease();

        if (isFull()) {
            throw new BufferOverflowException();
        }

        if(isEmpty() && readPositon!=0){
            clear();
        }
        unsafe.putByte(address + writePositon, b);
        writePositon = (writePositon + 1) % capacity;
    }

    /**
     * 写入一个 byte[] 数据
     * @param bytes byte[] 对象
     * @param offset 针对 byte[] 的偏移量
     * @param length 写入数据的长度
     * @return 写入数据长度
     */
    public int write(byte[] bytes, int offset, int length){
        checkRelease();

        if(length > avaliable()){
            throw new BufferOverflowException();
        }

        for(int i=0;i<length;i++){
            write(bytes[offset + i]);
        }

        return length;
    }

    /**
     * 写入一个 byteBuffer
     * @param byteBuffer ByteBuffer 对象
     * @return 写入数据长度
     */
    public int write(ByteBuffer byteBuffer) {
        checkRelease();

        if(byteBuffer.remaining() == 0){
            return 0;
        }

        if(byteBuffer.remaining() > avaliable()){
            throw new BufferOverflowException();
        }

        if (byteBuffer == null) {
            return -1;
        }

        int writeSize = byteBuffer.remaining();

        int size = 0;
        while(writeSize > 0){
            write(byteBuffer.get());
            size++;
            writeSize--;
        }

        return size;
    }

    /**
     * 缓冲区可读数据量
     * @return 缓冲区可用数据量
     */
    public int remaining() {
        checkRelease();

        if(writePositon == readPositon){
            return 0;
        } else if(writePositon < readPositon){
            return capacity - readPositon + writePositon;
        } else {
            return writePositon - readPositon;
        }
    }

    /**
     * 缓冲区可写空间
     * @return 缓冲区可写空间
     */
    public int avaliable() {
        checkRelease();

        return capacity - remaining() - 1;
    }

    /**
     * 读取一个 byte
     * @return byte 数据
     */
    public byte read() {
        checkRelease();

        if (isEmpty()) {
            throw new BufferUnderflowException();
        }
        byte result = unsafe.getByte(address + readPositon);
        readPositon = (readPositon + 1) % capacity;

        if(isEmpty() && readPositon!=0){
            clear();
        }
        return result;
    }

    /**
     * 读取缓冲区的数据
     * @param byteBuffer ByteBuffer 对象
     * @return 读取数据大小
     */
    public int read(ByteBuffer byteBuffer) {
        checkRelease();

        int size = byteBuffer.remaining();
        if(size > remaining()){
            size = remaining();
        }

        for(int i=0;i<size;i++){
            byteBuffer.put(read());
        }

        return size;
    }

    /**
     * 读取缓冲区的数据
     * @param bytes 用于读取数据的 byte 数组
     * @param offset 偏移量
     * @param length 读取数据的长度
     * @return 读取数据大小
     */
    public int read(byte[] bytes, int offset, int length) {
        checkRelease();

        if(length > remaining()){
            length = remaining();
        }

        for(int i=offset;i<offset+length;i++){
            bytes[i] = read();
        }

        return length;
    }

    /**
     * 获得一个只读的 ByteBuffer 类型的数据
     * @return ByteBuffer 对象
     */
    public ByteBuffer getByteBuffer() {
        checkRelease();

	    if(writePositon < readPositon) {
		    int remaining = remaining();
		    int tailSize = capacity - readPositon;

		    byteBuffer.position(0);
		    byteBuffer.limit(writePositon);
		    byte[] tmp = TByteBuffer.toArray(byteBuffer);
		    byteBuffer.limit(byteBuffer.capacity());

		    unsafe.copyMemory(address + readPositon, address, tailSize);

		    for(int i = 0; i<tmp.length; i++){
			    unsafe.putByte(address + tailSize + i, tmp[i]);
		    }

		    readPositon = 0;
		    writePositon = remaining;
	    }

	    byteBuffer.limit(writePositon);
	    byteBuffer.position(readPositon);

        return byteBuffer;
    }

    public void compact(){
        checkRelease();
        readPositon = byteBuffer.position();
        writePositon = byteBuffer.limit();
    }

    /**
     * 查找特定 byte 标识的位置
     *     byte 标识数组第一个字节的索引位置
     * @param mark byte 标识数组
     * @return 第一个字节的索引位置
     */
    public int indexOf(byte[] mark){
        checkRelease();

        if(remaining() == 0){
            return -1;
        }

        int length = remaining();

        if(length < mark.length){
            return -1;
        }

        int index = -1;

        int i = 0;
        int j = 0;

        while(i <= (remaining() - mark.length + j )  ){
            if(get(i) != mark[j] ){
                if(i == (remaining() - mark.length + j )){
                    break;
                }
                int pos = TStream.contains(mark, get(i+mark.length-j));
                if( pos== -1){
                    i = i + mark.length + 1 - j;
                    j = 0 ;
                }else{
                    i = i + mark.length - pos - j;
                    j = 0;
                }
            }else{
                if(j == (mark.length - 1)){
                    i = i - j + 1 ;
                    j = 0;
                    index  = i-j - 1;
                    break;
                }else{
                    i++;
                    j++;
                }
            }
        }

        return index == -1 ? index : (index + readPositon) % capacity;
    }

    public boolean startWith(byte[] mark){
        checkRelease();

        if(remaining() < mark.length){
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
     * 等待期望的数据长度
     * @param length  期望的数据长度
     * @param timeout 超时时间,单位: 毫秒
     * @param supplier 每次等待数据所做的操作
     * @return true: 具备期望长度的数据, false: 等待数据超时
     */
    public boolean waitData(int length,int timeout, Runnable supplier){
        checkRelease();

        try {
            TEnv.wait(timeout, ()->{

                if(remaining() >= length){
                    return false;
                } else {
                    supplier.run();
                    return remaining() < length;
                }
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }


    /**
     * 从头部开始判断是否收到期望的数据
     * @param mark  期望出现的数据
     * @param timeout 超时时间,单位: 毫秒
     * @param supplier 每次等待数据所做的操作
     * @return true: 具备期望长度的数据, false: 等待数据超时
     */
    public boolean waitData(byte[] mark, int timeout, Runnable supplier){
        checkRelease();

        try {
            TEnv.wait(timeout, ()->{
                if(indexOf(mark) != -1) {
                    return false;
                } else {
                    supplier.run();
                    return indexOf(mark) == -1;
                }
            });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * 读取一行
     * @return 字符串
     */
    public String readLine() {
        checkRelease();

        if(remaining() == 0){
            return null;
        }

        String lineStr = null;

        int index = indexOf("\n".getBytes());

        int size = -1;

        if (index >= 0) {
			if(readPositon > index) {
				size = capacity - readPositon + index;
			} else {
				size = index - readPositon;
			}

			size++;


        }

        if(remaining()>0 && index==-1){
            size = remaining();
        }

        byte[] temp = new byte[size];

        for(int i=0;i<size; i++){
            temp[i] = read();
        }

        lineStr = new String(temp);
        return lineStr.isEmpty() && index==-1 ? null : lineStr;
    }

    /**
     * 保存到文件
     * @param filePath 文件路径
     * @param length 需要保存的长度
     * @throws IOException Io 异常
     */
    public void saveToFile(String filePath, long length) throws IOException{
        checkRelease();

        if(remaining() == 0){
            return;
        }

        int bufferSize = 1024 * 1024;

        if (length < bufferSize) {
            bufferSize = Long.valueOf(length).intValue();
        }

        byte[] buffer = new byte[bufferSize];

        TFile.mkdir(TFile.getFileDirectory(filePath));
        RandomAccessFile randomAccessFile = null;
        File file = new File(filePath);


        try {
            randomAccessFile = new RandomAccessFile(file, "rwd");

            //追加形式
            randomAccessFile.seek(randomAccessFile.length());

            int loadSize = bufferSize;
            ByteBuffer tempByteBuffer = ByteBuffer.wrap(buffer);

            while (length > 0) {
                loadSize = length > bufferSize ? bufferSize : Long.valueOf(length).intValue();

                tempByteBuffer.limit(loadSize);

                this.read(tempByteBuffer);

                randomAccessFile.write(buffer, 0, loadSize);

                length = length - loadSize;

                tempByteBuffer.clear();
            }
        } catch (IOException e) {
            throw e;
        } finally {
            randomAccessFile.close();
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

    public boolean isReleased(){
        if(address == 0 || byteBuffer == null){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 释放内存中的数据
     */
    public synchronized void release(){
        address = 0;
        byteBuffer = null;
        TByteBuffer.release(byteBuffer);
    }

    @Override
    public String toString(){
        return "readPositon=" + readPositon+", writePositon="+writePositon+", capacity="+capacity+", remaining="+remaining()+", avaliable="+avaliable()+", address="+address;
    }
}
