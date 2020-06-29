package org.voovan.tools.buffer;

import org.voovan.Global;
import org.voovan.tools.*;
import org.voovan.tools.collection.ThreadObjectPool;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * ByteBuffer 工具类
 *
 * @author helyho
 *
 * Java Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TByteBuffer {
    public final static int DEFAULT_BYTE_BUFFER_SIZE = TEnv.getSystemProperty("ByteBufferSize", 1024*8);
    public final static int THREAD_BUFFER_POOL_SIZE  = TEnv.getSystemProperty("ThreadBufferPoolSize", 64);
    public final static LongAdder MALLOC_SIZE       = new LongAdder();
    public final static LongAdder MALLOC_COUNT      = new LongAdder();
    public final static LongAdder BYTE_BUFFER_COUNT = new LongAdder();

    public final static int BYTE_BUFFER_ANALYSIS  = TEnv.getSystemProperty("ByteBufferAnalysis", 0);

    public static void malloc(int capacity) {
        if(BYTE_BUFFER_ANALYSIS >= 0) {
            MALLOC_SIZE.add(capacity);
            MALLOC_COUNT.increment();
            BYTE_BUFFER_COUNT.increment();
        }
    }

    public static void realloc(int oldCapacity, int newCapacity) {
        if(BYTE_BUFFER_ANALYSIS >= 0) {
            MALLOC_SIZE.add(newCapacity - oldCapacity);
        }
    }


    public static void free(int capacity) {
        if(BYTE_BUFFER_ANALYSIS >= 0) {
            MALLOC_SIZE.add(-1 * capacity);
            MALLOC_COUNT.decrement();
            BYTE_BUFFER_COUNT.decrement();
        }
    }

    public static Map<String, Long> getByteBufferAnalysis() {
       return TObject.asMap("Time", TDateTime.now(), "MallocSize", TString.formatBytes(MALLOC_SIZE.longValue()),
               "MallocCount", MALLOC_COUNT.longValue(),
               "ByteBufferCount", BYTE_BUFFER_COUNT.longValue());
    }

    static {
        if(BYTE_BUFFER_ANALYSIS > 0) {
            Global.getHashWheelTimer().addTask(() -> {
                Logger.simple(getByteBufferAnalysis());
            }, BYTE_BUFFER_ANALYSIS);
        }
    }

    public final static ThreadObjectPool<ByteBuffer> THREAD_BYTE_BUFFER_POOL = new ThreadObjectPool<ByteBuffer>(THREAD_BUFFER_POOL_SIZE, ()->allocateManualReleaseBuffer(DEFAULT_BYTE_BUFFER_SIZE));

    static {
        System.out.println("[SYTSEM] ThreadBufferPoolSize: " + THREAD_BYTE_BUFFER_POOL.getThreadPoolSize());
        System.out.println("[SYTSEM] BufferSize: " + DEFAULT_BYTE_BUFFER_SIZE);
    }

    public final static ByteBuffer EMPTY_BYTE_BUFFER = ByteBuffer.allocateDirect(0);

    public final static Class DIRECT_BYTE_BUFFER_CLASS = EMPTY_BYTE_BUFFER.getClass();

    public final static Constructor DIRECT_BYTE_BUFFER_CONSTURCTOR = getConsturctor();
    static {
        DIRECT_BYTE_BUFFER_CONSTURCTOR.setAccessible(true);
    }

    public final static Field addressField = ByteBufferField("address");
    public final static Field capacityField = ByteBufferField("capacity");
    public final static Field attField = ByteBufferField("att");

    private static Constructor getConsturctor(){
        int paramCount = TEnv.JDK_VERSION >= 14 ? 4 : 3;
        Constructor[] constructor = TReflect.findConstructor(DIRECT_BYTE_BUFFER_CLASS, paramCount);

        constructor[0].setAccessible(true);
        return constructor[0];
    }

    private static Field ByteBufferField(String fieldName){
        Field field = TReflect.findField(DIRECT_BYTE_BUFFER_CLASS, fieldName);
        field.setAccessible(true);
        return field;
    }

    /**
     * 分配可能手工进行释放的 ByteBuffer
     * @param capacity 容量
     * @return ByteBuffer 对象
     */
    protected static ByteBuffer allocateManualReleaseBuffer(int capacity){
        try {
            long address = (TUnsafe.getUnsafe().allocateMemory(capacity));

            Deallocator deallocator = new Deallocator(address, capacity);

            ByteBuffer byteBuffer = null;
            if(DIRECT_BYTE_BUFFER_CONSTURCTOR.getParameterCount() == 3) {
                byteBuffer = (ByteBuffer) DIRECT_BYTE_BUFFER_CONSTURCTOR.newInstance(address, capacity, deallocator);
            } else {
                //jdk 14 兼容
                byteBuffer = (ByteBuffer) DIRECT_BYTE_BUFFER_CONSTURCTOR.newInstance(address, capacity, deallocator, null);
            }

            Cleaner.create(byteBuffer, deallocator);


            malloc(capacity);

            return byteBuffer;

        } catch (Exception e) {
            Logger.error("Allocate ByteBuffer error. ", e);
            return null;
        }
    }

    /**
     * 根据框架的非堆内存配置, 分配 ByteBuffer
     * @return ByteBuffer 对象
     */
    public static ByteBuffer allocateDirect() {
        return allocateDirect(DEFAULT_BYTE_BUFFER_SIZE);
    }

    /**
     * 根据框架的非堆内存配置, 分配 ByteBuffer
     * @param capacity 容量
     * @return ByteBuffer 对象
     */
    public static ByteBuffer allocateDirect(int capacity) {

        ByteBuffer byteBuffer = null;

        while (byteBuffer == null) {
            byteBuffer = THREAD_BYTE_BUFFER_POOL.get(() -> allocateManualReleaseBuffer(capacity));

            try {
                if (capacity <= byteBuffer.capacity()) {
                    byteBuffer.limit(capacity);
                } else {
                    reallocate(byteBuffer, capacity);
                }

                byteBuffer.position(0);
                byteBuffer.limit(capacity);
            } catch (Exception e) {
                byteBuffer = null;
                if(byteBuffer!=null) {
                    TByteBuffer.release(byteBuffer);
                }
            }
        }

        return byteBuffer;

    }

    /**
     * 重新分配 byteBuffer 中的空间大小
     * @param byteBuffer byteBuffer对象
     * @param newSize  重新分配的空间大小
     * @return true:成功, false:失败
     */
    public static boolean reallocate(ByteBuffer byteBuffer, int newSize) {

        if(isReleased(byteBuffer)) {
            return false;
        }

        try {
            int oldCapacity = byteBuffer.capacity();

            if(oldCapacity > newSize){
                byteBuffer.limit(newSize);
                return true;
            }

            if(!byteBuffer.hasArray()) {
                if(getAtt(byteBuffer) == null){
                    throw new UnsupportedOperationException("JDK's ByteBuffer can't reallocate");
                }
                long address = getAddress(byteBuffer);
                long newAddress = TUnsafe.getUnsafe().reallocateMemory(address, newSize);
                setAddress(byteBuffer, newAddress);
            }else{
                byte[] hb = byteBuffer.array();
                byte[] newHb = Arrays.copyOf(hb, newSize);
                TReflect.setFieldValue(byteBuffer, "hb", newHb);
            }

            //重置容量
            capacityField.set(byteBuffer, newSize);

            realloc(oldCapacity, newSize);
            return true;

        }catch (ReflectiveOperationException e){
            Logger.error("TByteBuffer.reallocate() Error. ", e);
        }
        return false;
    }

    /**
     * 移动 Bytebuffer 中的数据
     *       以Bytebuffer.position()为原点,移动 offset 个位置
     * @param byteBuffer byteBuffer对象
     * @param offset 相对当前 ByteBuffer.position 的偏移量
     * @return true:成功, false:失败
     */
    public static boolean move(ByteBuffer byteBuffer, int offset) {
        try {
            if(byteBuffer.remaining() == 0) {
                byteBuffer.position(0);
                byteBuffer.limit(0);
                return true;
            }

            if(offset==0){
                return true;
            }

            int newPosition = byteBuffer.position() + offset;
            int newLimit = byteBuffer.limit() + offset;

            if(newPosition < 0){
                return false;
            }

            if(newLimit > byteBuffer.capacity()){
                reallocate(byteBuffer, newLimit);
            }

            if(!byteBuffer.hasArray()) {
                long address = getAddress(byteBuffer);
                if(address!=0) {
                    long startAddress = address + byteBuffer.position();
                    long targetAddress = address + newPosition;
                    if (address > targetAddress) {
                        targetAddress = address;
                    }
                    TUnsafe.getUnsafe().copyMemory(startAddress, targetAddress, byteBuffer.remaining());
                }
            }else{
                byte[] hb = byteBuffer.array();
                System.arraycopy(hb, byteBuffer.position(), hb, newPosition, byteBuffer.remaining());
            }

            byteBuffer.limit(newLimit);
            byteBuffer.position(newPosition);
            return true;
        }catch (ReflectiveOperationException e){
            Logger.error("TByteBuffer.moveData() Error.", e);
        }
        return false;
    }

    /**
     * 复制一个 Bytebuffer 对象
     * @param byteBuffer 原 ByteBuffer 对象
     * @return 复制出的对象
     * @throws ReflectiveOperationException 反射错误
     */
    public static ByteBuffer copy(ByteBuffer byteBuffer) throws ReflectiveOperationException {

        ByteBuffer newByteBuffer = TByteBuffer.allocateDirect(byteBuffer.capacity());

        if(byteBuffer.hasRemaining()) {
            long address = getAddress(byteBuffer);
            long newAddress = getAddress(newByteBuffer);
            TUnsafe.getUnsafe().copyMemory(address, newAddress + byteBuffer.position(), byteBuffer.remaining());
        }

        newByteBuffer.position(byteBuffer.position());
        newByteBuffer.limit(byteBuffer.limit());

        return newByteBuffer;
    }

    /**
     * 在 srcBuffer 后追加 appendBuffer;
     * @param srcBuffer 被追加数据的 srcBuffer
     * @param srcPosition 被追加数据的位置
     * @param appendBuffer 追加的内容
     * @param appendPosition 追加的数据起始位置
     * @param length 追加数据的长度
     * @return 返回被追加数据的 srcBuffer
     */
    public static ByteBuffer append(ByteBuffer srcBuffer, int srcPosition, ByteBuffer appendBuffer, int appendPosition, int length) {
        try {
            int appendSize = appendBuffer.limit() < length ? appendBuffer.limit() : length;

            long srcAddress = getAddress(srcBuffer) + srcPosition;
            long appendAddress = getAddress(appendBuffer) + appendPosition;

            int availableSize = srcBuffer.capacity() - srcBuffer.limit();
            if (availableSize < appendSize) {
                int newSize = srcBuffer.capacity() + appendSize;
                reallocate(srcBuffer, newSize);
            }

            TUnsafe.getUnsafe().copyMemory(appendAddress, srcAddress, appendSize);

            srcBuffer.limit(srcPosition + appendSize);
            srcBuffer.position(srcBuffer.limit());

            return srcBuffer;
        } catch (ReflectiveOperationException e){
            Logger.error("TByteBuffer.moveData() Error.", e);
        }

        return null;
    }

    /**
     * 释放byteBuffer
     *      释放对外的 bytebuffer
     * @param byteBuffer bytebuffer 对象
     */
    public static void release(ByteBuffer byteBuffer) {
        if(byteBuffer == null){
            return;
        }

        if (byteBuffer != null) {

            if(THREAD_BYTE_BUFFER_POOL.getPool().avaliable() > 0 &&
                    byteBuffer.capacity() > DEFAULT_BYTE_BUFFER_SIZE){
                reallocate(byteBuffer, DEFAULT_BYTE_BUFFER_SIZE);
            }

            THREAD_BYTE_BUFFER_POOL.release(byteBuffer, (buffer)->{
                try {
                    long address = TByteBuffer.getAddress(byteBuffer);
                    Object att = getAtt(byteBuffer);
                    if (address!=0 && att!=null && att.getClass() == Deallocator.class) {
                        if(address!=0) {
                            byteBuffer.clear();
                            synchronized (byteBuffer) {
                                //这里不使用传入的参数, 需要复用上面代码获得的地址
                                byteBuffer.position(0);
                                byteBuffer.limit(0);
                                setCapacity(byteBuffer, 0);
                                setAddress(byteBuffer, 0);

                                TUnsafe.getUnsafe().freeMemory(address);
                                free(byteBuffer.capacity());
                            }
                        }
                    }
                } catch (ReflectiveOperationException e) {
                    Logger.error(e);
                }
            });
        }
    }


    /**
     * 判断是否已经释放
     * @param byteBuffer ByteBuffer 对象
     * @return true: 已释放, false: 未释放
     */
    public static boolean isReleased(ByteBuffer byteBuffer){

        if(byteBuffer==null){
            return true;
        }

        try {
            return getAddress(byteBuffer) == 0;
        }catch (ReflectiveOperationException e){
            return true;
        }
    }

    /**
     * 将ByteBuffer转换成 byte 数组
     * @param bytebuffer ByteBuffer 对象
     * @return byte 数组
     */
    public static byte[] toArray(ByteBuffer bytebuffer){
        if(!bytebuffer.hasArray()) {
            if(isReleased(bytebuffer)) {
                return new byte[0];
            }

            bytebuffer.mark();
            int position = bytebuffer.position();
            int limit = bytebuffer.limit();
            byte[] buffers = new byte[limit-position];
            bytebuffer.get(buffers);
            bytebuffer.reset();
            return buffers;
        }else{
            return Arrays.copyOfRange(bytebuffer.array(), 0, bytebuffer.limit());
        }
    }

    /**
     * 将 Bytebuffer 转换成 字符串
     * @param bytebuffer Bytebuffer 对象
     * @param charset 字符集
     * @return 字符串对象
     */
    public static String toString(ByteBuffer bytebuffer,String charset) {
        try {
            return new String(toArray(bytebuffer), charset);
        } catch (UnsupportedEncodingException e) {
            Logger.error(charset+" is not supported",e);
            return null;
        }
    }

    /**
     * 将 Bytebuffer 转换成 字符串
     * @param bytebuffer Bytebuffer 对象
     * @return 字符串对象
     */
    public static String toString(ByteBuffer bytebuffer) {
        return toString(bytebuffer, "UTF-8");
    }

    /**
     * 查找特定 byte 标识的位置
     *     byte 标识数组第一个字节的索引位置
     * @param byteBuffer Bytebuffer 对象
     * @param mark byte 标识数组
     * @return 第一个字节的索引位置
     */
    public static int indexOf(ByteBuffer byteBuffer, byte[] mark){

        if(isReleased(byteBuffer)) {
            return -1;
        }

        if(byteBuffer.remaining() == 0){
            return -1;
        }

        int originPosition = byteBuffer.position();
        int length = byteBuffer.remaining();

        if(length < mark.length){
            return -1;
        }

        int index = -1;

        int i = byteBuffer.position();
        int j = 0;

        while(i <= (byteBuffer.limit() - mark.length + j )  ){
            if(byteBuffer.get(i) != mark[j] ){
                if(i == (byteBuffer.limit() - mark.length + j )){
                    break;
                }
                int pos = TByte.byteIndexOf(mark, byteBuffer.get(i+mark.length-j));
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

        byteBuffer.position(originPosition);

        return index;
    }


    /**
     * 获取内存地址
     * @param byteBuffer bytebuffer 对象
     * @return 内存地址
     * @throws ReflectiveOperationException 反射异常
     */
    public static Long getAddress(ByteBuffer byteBuffer) throws ReflectiveOperationException {
        return (Long) addressField.get(byteBuffer);
    }

    /**
     * 设置内存地址
     * @param byteBuffer bytebuffer 对象
     * @param address 内存地址
     * @throws ReflectiveOperationException 反射异常
     */
    public static void setAddress(ByteBuffer byteBuffer, long address) throws ReflectiveOperationException {
        addressField.set(byteBuffer, address);
        Object att = getAtt(byteBuffer);
        if(att!=null && att.getClass() == Deallocator.class){
            ((Deallocator) att).setAddress(address);
        }
    }

    /**
     * 获取附加对象
     * @param byteBuffer bytebuffer 对象
     * @return 附加对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static Object getAtt(ByteBuffer byteBuffer) throws ReflectiveOperationException {
        return attField.get(byteBuffer);
    }

    /**
     * 设置附加对象
     * @param byteBuffer bytebuffer 对象
     * @param attr 附加对象
     * @throws ReflectiveOperationException 反射异常
     */
    public static void setAttr(ByteBuffer byteBuffer, Object attr) throws ReflectiveOperationException {
        attField.set(byteBuffer, attr);
    }

    /**
     * 设置内存地址
     * @param byteBuffer bytebuffer 对象
     * @param capacity 容量
     * @throws ReflectiveOperationException 反射异常
     */
    public static void setCapacity(ByteBuffer byteBuffer, int capacity) throws ReflectiveOperationException {
        capacityField.set(byteBuffer, capacity);
        Object att = getAtt(byteBuffer);
        if(att!=null && att.getClass() == Deallocator.class){
            ((Deallocator) att).setCapacity(capacity);
        }
    }
}
