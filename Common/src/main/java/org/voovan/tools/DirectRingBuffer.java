package org.voovan.tools;

import sun.misc.Unsafe;

import java.io.IOException;
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
     * 缓冲区空判断
     * @return true: 缓冲区无可用数据, false: 缓冲区有可用数据
     */
    private Boolean isEmpty() {
        return readPositon == writePositon;
    }

    /**
     * 缓冲区满判断
     * @return true: 缓冲区已满, false: 缓冲区未满
     */
    private Boolean isFull() {
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
     * @throws IOException IO 异常
     */
    public int getAll(ByteBuffer byteBuffer) throws IOException {
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
     * @throws IOException IO 异常
     */
    public int getAll(byte[] bytes, int offset) throws IOException {
        int size = bytes.length - offset;
        if(size > remaining()){
            size = remaining();
        }

        for(int i=offset;i<offset+size;i++){
            bytes[i] = get(i);
        }

        return size;
    }


    /**
     * 读取所有缓冲区的数据
     *      不影响读写位置
     * @return 读取数据
     * @throws IOException IO 异常
     */
    public byte[] getAll() throws IOException {
        byte[] bytes = new byte[remaining()];
        getAll(bytes, 0);
        return bytes;
    }

    /**
     * 写入一个 byte
     * @param b byte 数据
     * @throws IOException IO 异常
     */
    public void write(byte b) throws IOException {
        if (isFull()) {
            throw new IOException("Buffer is full");
        }

        unsafe.putByte(address + writePositon, b);
        writePositon = (writePositon + 1) % capacity;
    }

    /**
     * 读取一个 byte
     * @return byte 数据
     * @throws IOException IO 异常
     */
    public byte read() throws IOException {
        if (isEmpty()) {
            throw new IOException("Not enough data");
        }
        byte result = unsafe.getByte(address+readPositon);
        readPositon = (readPositon + 1) % capacity;
        return result;
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
     * 读取所有缓冲区的数据
     * @param byteBuffer ByteBuffer 对象
     * @return 读取数据大小
     * @throws IOException IO 异常
     */
    public int readAll(ByteBuffer byteBuffer) throws IOException {
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
     * 读取所有缓冲区的数据
     * @param bytes 用于读取数据的 byte 数组
     * @param offset 偏移量
     * @return 读取数据大小
     * @throws IOException IO 异常
     */
    public int readAll(byte[] bytes, int offset) throws IOException {
        int size = bytes.length - offset;
        if(size > remaining()){
            size = remaining();
        }

        for(int i=offset;i<offset+size;i++){
            bytes[i] = read();
        }

        return size;
    }

    /**
     * 读取所有缓冲区的数据
     * @return 读取的字节数组
     * @throws IOException IO 异常
     */
    public byte[] readAll() throws IOException {
        byte[] bytes = new byte[remaining()];
        readAll(bytes, 0);
        return bytes;
    }

    /**
     * 重新分配缓冲区的容量
     * @param newCapacity 新的缓冲区容量
     */
    public synchronized void resize(int newCapacity){
        if(capacity >= newCapacity){
            return;
        } else {
            address = unsafe.reallocateMemory(address, capacity);
            if(writePositon < readPositon){
                int capacityDiff = newCapacity - capacity;
                writePositon = writePositon > capacityDiff ? writePositon - capacityDiff : capacity + writePositon;
                unsafe.copyMemory(address, address + capacity, writePositon-1);
            }
            this.capacity = newCapacity;
        }
    }

    /**
     * 释放内存中的数据
     */
    public void release(){
        unsafe.freeMemory(address);
    }

    @Override
    public String toString(){
        return "readPositon=" + readPositon+", writePositon="+writePositon+", capacity="+capacity+", remaining="+remaining()+", avaliable="+avaliable()+", address="+address;
    }

    public static void main(String[] args) throws IOException {
        DirectRingBuffer directRingBuffer = new DirectRingBuffer(10);

        int i=0;
        while(directRingBuffer.avaliable() > 0) {
            i++;
            directRingBuffer.write((byte) (60+i));
        }
        System.out.println(i);
        byte[] a = new byte[directRingBuffer.capacity];
        directRingBuffer.readAll(a, 5);

        directRingBuffer.write((byte)80);
        directRingBuffer.write((byte)80);
        directRingBuffer.write((byte)80);
        directRingBuffer.write((byte)80);
        directRingBuffer.resize(30);

        i=0;
        while(directRingBuffer.avaliable() > 0) {
            directRingBuffer.write((byte) (70+i));
        }

        ByteBuffer b = ByteBuffer.wrap(a);
        directRingBuffer.readAll(b);
        TEnv.sleep(100000);
    }
}
