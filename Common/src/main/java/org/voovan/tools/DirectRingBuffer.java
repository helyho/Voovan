package org.voovan.tools;

import sun.misc.Unsafe;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * 环形缓冲区
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DirectRingBuffer {
    private static Unsafe unsafe = TUnsafe.getUnsafe();
    private TByteBuffer.Deallocator deallocator;
    private long address;
    private int readPositon = 0;
    private int writePositon = 0;
    private int capacity;

    /**
     * 使用默认容量构造一个环形缓冲区
     */
    public DirectRingBuffer(){
        this(TByteBuffer.DEFAULT_BYTE_BUFFER_SIZE);
    }

    /**
     * 使用指定容量构造一个环形缓冲区
     * @param capacity
     */
    public DirectRingBuffer(int capacity){
        this.capacity = capacity;
        this.address = unsafe.allocateMemory(capacity);

        //构造自动销毁器
        deallocator = new TByteBuffer.Deallocator(new Long(address));
        Cleaner.create(this, deallocator);
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
    public void skip(int offset){
        if(remaining() < offset){
            throw new BufferOverflowException();
        }

        readPositon = ( readPositon + offset ) % capacity;
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
    public void clear(){
        this.readPositon = 0;
        this.writePositon = 0;
    }

    /**
     * 获得基于索引位置的数据
     * @param offset 偏移量
     * @return byte 数据
     */
    public byte get(int offset){
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
     * @return 读取数据大小
     */
    public int get(byte[] bytes, int offset, int length) {
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
        if (isFull()) {
            throw new BufferOverflowException();
        }

        unsafe.putByte(address + writePositon, b);
        writePositon = (writePositon + 1) % capacity;
    }

    /**
     * 写入一个 byte[] 数据
     * @param bytes byte[] 对象
     * @param offset 针对 byte[] 的偏移量
     * @param length 写入数据的长度
     */
    public int write(byte[] bytes, int offset, int length){
        if(length > remaining()){
            length = remaining();
        }

        for(int i=0;i<length;i++){
            write(bytes[offset + i]);
        }

        return length;
    }

    /**
     * 写入一个 byteBuffer
     * @param byteBuffer ByteBuffer 对象
     */
    public int write(ByteBuffer byteBuffer) {
        if(byteBuffer.remaining() == 0){
            return 0;
        }

        int writeSize = byteBuffer.remaining();

        if(byteBuffer.remaining() > avaliable()){
            writeSize = avaliable();
        }

        if (byteBuffer == null) {
            return -1;
        }

        int size = 0;
        while(writeSize > 0){
            write(byteBuffer.get());
            size++;
            writeSize--;
        }

        return size;
    }

    /**
     * 缓冲区可用数据量
     * @return 缓冲区可用数据量
     */
    public int remaining(){
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
    public int avaliable(){
        return capacity - remaining() - 1;
    }

    /**
     * 读取一个 byte
     * @return byte 数据
     */
    public byte read() {
        if (isEmpty()) {
            throw new BufferUnderflowException();
        }
        byte result = unsafe.getByte(address+readPositon);
        readPositon = (readPositon + 1) % capacity;
        return result;
    }

    /**
     * 读取缓冲区的数据
     * @param byteBuffer ByteBuffer 对象
     * @return 读取数据大小
     */
    public int read(ByteBuffer byteBuffer) {
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
        if(length > remaining()){
            length = remaining();
        }

        for(int i=offset;i<offset+length;i++){
            bytes[i] = read();
        }

        return length;
    }

    /**
     * 重新分配缓冲区的容量
     * @param newCapacity 新的缓冲区容量
     */
    public synchronized boolean resize(int newCapacity){
        if(capacity >= newCapacity){
            return false;
        } else {
            address = unsafe.reallocateMemory(address, capacity);
            if(writePositon < readPositon){
                int capacityDiff = newCapacity - capacity;
                writePositon = writePositon > capacityDiff ? writePositon - capacityDiff : capacity + writePositon;
                unsafe.copyMemory(address, address + capacity, writePositon-1);
            }
            this.capacity = newCapacity;
            return true;
        }
    }

    /**
     * 获得一个只读的 ByteBuffer 类型的数据
     * @return ByteBuffer 对象
     * @throws ReflectiveOperationException 反射异常
     */
    public ByteBuffer asByteBuffer() throws ReflectiveOperationException {
        if(writePositon < readPositon) {
            unsafe.copyMemory(address + readPositon, address, remaining());
        }
        ByteBuffer byteBuffer = TByteBuffer.allocateDirect(0).asReadOnlyBuffer();
        TByteBuffer.capacityField.set(byteBuffer, capacity);
        TByteBuffer.limitField.set(byteBuffer, writePositon);
        byteBuffer.position(readPositon);
        TByteBuffer.setAddress(byteBuffer, address);
        return byteBuffer;
    }

    /**
     * 查找特定 byte 标识的位置
     *     byte 标识数组第一个字节的索引位置
     * @param mark byte 标识数组
     * @return 第一个字节的索引位置
     */
    public int indexOf(byte[] mark){

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

        return index;
    }

    /**
     * 释放内存中的数据
     */
    public void release(){
        unsafe.freeMemory(address);
        address = 0;
    }

    @Override
    public String toString(){
        return "readPositon=" + readPositon+", writePositon="+writePositon+", capacity="+capacity+", remaining="+remaining()+", avaliable="+avaliable()+", address="+address;
    }
}
