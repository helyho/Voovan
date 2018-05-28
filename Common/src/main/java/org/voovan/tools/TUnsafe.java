package org.voovan.tools;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class TUnsafe {

    private static Unsafe unsafe;

    private static ConcurrentHashMap<Field, Long> OFFSET_MAP = new ConcurrentHashMap<Field, Long>();

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Unsafe getUnsafe() {
        return unsafe;
    }

    public static Long getFieldOffset(Field field){
        Long offset = OFFSET_MAP.get(field);
        if(offset==null){
            offset = unsafe.objectFieldOffset(field);
            OFFSET_MAP.put(field, offset);
        }
        return offset;
    }

    public static <T> T allocateInstance(Class clazz) throws InstantiationException {
        return (T) TUnsafe.getUnsafe().allocateInstance(clazz);
    }

    public static void putObject(Object obj, Field field, Object value){
        unsafe.putObject(obj, getFieldOffset(field), value);
    }

    public static void putByte(Object obj, Field field, byte value){
        unsafe.putByte(obj, getFieldOffset(field), value);
    }

    public static void putIntger(Object obj, Field field, int value){
        unsafe.putInt(obj, getFieldOffset(field), value);
    }

    public static void putShort(Object obj, Field field, short value){
        unsafe.putShort(obj, getFieldOffset(field), value);
    }

    public static void putBoolean(Object obj, Field field, boolean value){
        unsafe.putBoolean(obj, getFieldOffset(field), value);
    }

    public static void putLong(Object obj, Field field, long value){
        unsafe.putLong(obj, getFieldOffset(field), value);
    }

    public static void putFloat(Object obj, Field field, float value){
        unsafe.putFloat(obj, getFieldOffset(field), value);
    }

    public static void putDobule(Object obj, Field field, double value){
        unsafe.putDouble(obj, getFieldOffset(field), value);
    }


    public static <T> T getObject(Object obj, Field field){
        return (T)unsafe.getObject(obj, getFieldOffset(field));
    }

    public static byte getByte(Object obj, Field field){
        return unsafe.getByte(obj, getFieldOffset(field));
    }

    public static int putIntger(Object obj, Field field){
        return unsafe.getInt(obj, getFieldOffset(field));
    }

    public static short putShort(Object obj, Field field){
        return unsafe.getShort(obj, getFieldOffset(field));
    }

    public static boolean putBoolean(Object obj, Field field){
        return unsafe.getBoolean(obj, getFieldOffset(field));
    }

    public static long putLong(Object obj, Field field){
        return unsafe.getLong(obj, getFieldOffset(field));
    }

    public static float putFloat(Object obj, Field field){
        return unsafe.getFloat(obj, getFieldOffset(field));
    }

    public static double putDobule(Object obj, Field field){
        return unsafe.getDouble(obj, getFieldOffset(field));
    }


}
