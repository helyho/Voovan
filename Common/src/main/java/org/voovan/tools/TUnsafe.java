package org.voovan.tools;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

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

    public static Unsafe getUnsafe() {
        if(unsafe==null) {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                unsafe = (Unsafe) field.get(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return unsafe;
    }

    public static <T> T allocateInstance(Class clazz) throws InstantiationException {
       return (T) TUnsafe.getUnsafe().allocateInstance(clazz);
    }

    public static void putObject(Object obj, Field field, Object value){
        unsafe.putObject(obj, unsafe.objectFieldOffset(field), value);
    }

    public static void putByte(Object obj, Field field, byte value){
        unsafe.putByte(obj, unsafe.objectFieldOffset(field), value);
    }

    public static void putIntger(Object obj, Field field, int value){
        unsafe.putInt(obj, unsafe.objectFieldOffset(field), value);
    }

    public static void putShort(Object obj, Field field, short value){
        unsafe.putShort(obj, unsafe.objectFieldOffset(field), value);
    }

    public static void putBoolean(Object obj, Field field, boolean value){
        unsafe.putBoolean(obj, unsafe.objectFieldOffset(field), value);
    }

    public static void putLong(Object obj, Field field, long value){
        unsafe.putLong(obj, unsafe.objectFieldOffset(field), value);
    }

    public static void putFloat(Object obj, Field field, float value){
        unsafe.putFloat(obj, unsafe.objectFieldOffset(field), value);
    }

    public static void putDobule(Object obj, Field field, double value){
        unsafe.putDouble(obj, unsafe.objectFieldOffset(field), value);
    }


    public static <T> T getObject(Object obj, Field field){
        return (T)unsafe.getObject(obj, unsafe.objectFieldOffset(field));
    }

    public static byte getByte(Object obj, Field field){
        return unsafe.getByte(obj, unsafe.objectFieldOffset(field));
    }

    public static int putIntger(Object obj, Field field){
        return unsafe.getInt(obj, unsafe.objectFieldOffset(field));
    }

    public static short putShort(Object obj, Field field){
        return unsafe.getShort(obj, unsafe.objectFieldOffset(field));
    }

    public static boolean putBoolean(Object obj, Field field){
        return unsafe.getBoolean(obj, unsafe.objectFieldOffset(field));
    }

    public static long putLong(Object obj, Field field){
        return unsafe.getLong(obj, unsafe.objectFieldOffset(field));
    }

    public static float putFloat(Object obj, Field field){
        return unsafe.getFloat(obj, unsafe.objectFieldOffset(field));
    }

    public static double putDobule(Object obj, Field field){
        return unsafe.getDouble(obj, unsafe.objectFieldOffset(field));
    }


}
