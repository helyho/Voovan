package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import sun.misc.Cleaner;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Arrays;

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


    public static ByteBuffer allocateDirect(int capacity) {
        //是否手工释放
        if(Global.isNoHeapManualRelease()) {
            try {
                ByteBuffer template = TByteBuffer.allocateDirect(0);
                Constructor c = template.getClass().getDeclaredConstructor(long.class, int.class, Object.class);
                c.setAccessible(true);
                long address = (TUnsafe.getUnsafe().allocateMemory(capacity));

                return (ByteBuffer) c.newInstance(address, capacity, "VOOVAN");
            } catch (Exception e) {
                Logger.error("Create ByteBufferChannel error. ", e);
                return null;
            }
        }else{
            return TByteBuffer.allocateDirect(capacity);
        }
    }

    /**
     * 将ByteBuffer转换成 byte 数组
     * @param bytebuffer ByteBuffer 对象
     * @return byte 数组
     */
    public static byte[] toArray(ByteBuffer bytebuffer){
        if(!bytebuffer.hasArray()) {
            int oldPosition = bytebuffer.position();
            bytebuffer.position(0);
            int size = bytebuffer.limit();
            byte[] buffers = new byte[size];
            bytebuffer.get(buffers);
            bytebuffer.position(oldPosition);
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

        if(byteBuffer.remaining() == 0){
            return -1;
        }

        int index = -1;
        int position = byteBuffer.position();
        byte[] tmp = new byte[mark.length];
        int length = byteBuffer.remaining();
        for(int offset = 0; (offset + position <= length - mark.length); offset++){
            byteBuffer.position(position + offset);
            byteBuffer.get(tmp, 0, tmp.length);
            if(Arrays.equals(mark, tmp)){
                index = offset;
                break;
            }
        }

        byteBuffer.position(position);

        return index;
    }

    /**
     * 重新分配 byteBuffer 中的空间大小
     * @param byteBuffer byteBuffer对象
     * @param newSize  重新分配的空间大小
     * @return true:成功, false:失败
     */
    public static boolean reallocate(ByteBuffer byteBuffer, int newSize) {
        try {

            if(!byteBuffer.hasArray()) {
                long address = TReflect.getFieldValue(byteBuffer, "address");
                long newAddress = TUnsafe.getUnsafe().reallocateMemory(address, newSize);
                TReflect.setFieldValue(byteBuffer, "address", newAddress);

            }else{
                byte[] hb = byteBuffer.array();
                byte[] newHb = Arrays.copyOf(hb, newSize);
                TReflect.setFieldValue(byteBuffer, "hb", newHb);
            }

            //重置容量
            TReflect.setFieldValue(byteBuffer, "capacity", newSize);

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
    public static boolean moveData(ByteBuffer byteBuffer, int offset) {
        try {

            if(offset==0){
                return true;
            }

            if(byteBuffer.limit() == 0){
                return true;
            }

            int limit = byteBuffer.limit()+offset;
            int position = byteBuffer.position() + offset;

            if(limit > byteBuffer.capacity() || position < 0){
                return false;
            }

            if(!byteBuffer.hasArray()) {
                long address = TReflect.getFieldValue(byteBuffer, "address");
                if(address!=0) {
                    long startAddress = address + byteBuffer.position();
                    long targetAddress = address + position;
                    if (address > targetAddress) {
                        targetAddress = address;
                    }
                    TUnsafe.getUnsafe().copyMemory(startAddress, targetAddress, byteBuffer.remaining());
                }
            }else{
                byte[] hb = byteBuffer.array();
                System.arraycopy(hb, byteBuffer.position(), hb, position, byteBuffer.remaining());
            }

            TReflect.setFieldValue(byteBuffer, "limit", limit);
            byteBuffer.position(position);
            return true;
        }catch (ReflectiveOperationException e){
            Logger.error("TByteBuffer.moveData() Error.", e);
        }
        return false;
    }

    /**
     * 释放byteBuffer
     *      释放对外的 bytebuffer
     * @param byteBuffer bytebuffer 对象
     */
    public static void release(ByteBuffer byteBuffer){
        //是否手工释放
        if(!Global.isNoHeapManualRelease()) {
            return;
        }

        synchronized (byteBuffer) {
            try {
                if (byteBuffer != null && TReflect.findField(byteBuffer.getClass(), "address") != null) {
                    long address = (Long) TReflect.getFieldValue(byteBuffer, "address");
                    if (TReflect.findField(byteBuffer.getClass(), "att")!=null){
                        Object attr = TReflect.getFieldValue(byteBuffer, "att");
                        if (address != 0 && "VOOVAN".equals(attr)) {
                            TUnsafe.getUnsafe().freeMemory(address);
                            TReflect.setFieldValue(byteBuffer, "address", 0);
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        }
    }

}
