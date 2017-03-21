package org.voovan.tools;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.io.UnsupportedEncodingException;
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
     * 重新分配 byteBuffer 中的空间大小
     * @param byteBuffer byteBuffer对象
     * @param newSize  重新分配的空间大小
     * @return true:成功, false:失败
     */
    public static boolean reallocate(ByteBuffer byteBuffer, int newSize) {
        try {

            //计算新的 Limit 位置
            int newLimit = byteBuffer.limit();
            if (byteBuffer.limit() == byteBuffer.capacity()) {
                newLimit = newSize;
            }

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
            //重置 Limit 位置
            TReflect.setFieldValue(byteBuffer, "limit", newLimit);

            return true;

        }catch (ReflectiveOperationException e){
            Logger.error("TByteBuffer.reallocate() Error: "+e.getMessage(), e);
        }
        return false;
    }

    /**
     * 移动 Bytebuffer 中的数据
     *       以Bytebuffer.position()为远点,移动 offset 个位置
     * @param byteBuffer byteBuffer对象
     * @param offset 相对当前 ByteBuffer.position 的偏移量
     * @return true:成功, false:失败
     */
    public static boolean moveData(ByteBuffer byteBuffer, int offset) {
        try {
            int limit = byteBuffer.limit()+offset;
            int position = byteBuffer.position() + offset;

            if(limit > byteBuffer.capacity() && position < 0){
                return false;
            }

            if(!byteBuffer.hasArray()) {
                long address = TReflect.getFieldValue(byteBuffer, "address");
                TUnsafe.getUnsafe().copyMemory(address + byteBuffer.position(), address + position, byteBuffer.remaining());
            }else{
                byte[] hb = byteBuffer.array();
                System.arraycopy(hb, byteBuffer.position(), hb, position, byteBuffer.remaining());
            }

            TReflect.setFieldValue(byteBuffer, "limit", limit);
            byteBuffer.position(position);
            return true;
        }catch (ReflectiveOperationException e){
            Logger.error("TByteBuffer.moveData() Error: "+e.getMessage(), e);
        }
        return false;
    }

}
