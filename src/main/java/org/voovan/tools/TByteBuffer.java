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
            return Arrays.copyOfRange(bytebuffer.array(), bytebuffer.position(), bytebuffer.limit());
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

    public static long reallocateDirectByteBuffer(ByteBuffer byteBuffer, int newSize) {
        try {
            if(!byteBuffer.hasArray()) {
                long address = TReflect.getFieldValue(byteBuffer, "address");
                int newLimit = byteBuffer.limit();
                if (byteBuffer.limit() == byteBuffer.capacity()) {
                    newLimit = newSize;
                }

                long newAddress = TUnsafe.getUnsafe().reallocateMemory(address, newSize);
                TReflect.setFieldValue(byteBuffer, "address", newAddress);
                TReflect.setFieldValue(byteBuffer, "capacity", newSize);
                TReflect.setFieldValue(byteBuffer, "limit", newLimit);
                return newAddress;
            }
        }catch (ReflectiveOperationException e){
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean moveByteBufferData(ByteBuffer byteBuffer, int offset) {
        try {
            if(!byteBuffer.hasArray()) {
                long address = TReflect.getFieldValue(byteBuffer, "address");
                int limit = byteBuffer.limit()+offset;
                if(limit <= byteBuffer.capacity()) {
                    TUnsafe.getUnsafe().copyMemory(address + byteBuffer.position(), address + byteBuffer.position() + offset, byteBuffer.remaining());
                    TReflect.setFieldValue(byteBuffer, "limit", limit);
                    byteBuffer.position(0);
                }else{
                    return false;
                }
                return true;
            }
        }catch (ReflectiveOperationException e){
            e.printStackTrace();
        }
        return false;
    }
}
