package org.voovan.test;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import sun.jvm.hotspot.utilities.Bits;
import sun.misc.Unsafe;
import sun.misc.VM;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;


public class Other {


    public static void main(String[] args) throws Exception {
        int size = 5;
        Unsafe unsafe = getUnsafe();
        boolean pageAligned = VM.isDirectMemoryPageAligned();
        int pageSize = unsafe.pageSize();

        long base = unsafe.allocateMemory(size+ (pageAligned ? pageSize : 0) );
        unsafe.setMemory(base, size, (byte) 0);
        Logger.simple(base);
        for(int i=0;i<size;i++) {
            unsafe.putByte(base + i, (byte) (90+i) );
        }

        size = size*2;
        Logger.simple("b"+base);
        long mm = unsafe.reallocateMemory(base, size);
        Logger.simple("r"+mm);
        //源地址,目标地址,长度
        unsafe.copyMemory(base,base+5,5);
        //地址,长度,设置的值
        unsafe.setMemory(base, 5, (byte)0);

        for(int i=0;i<size;i++) {
            Logger.simple(unsafe.getByte(base+i));
        }

        unsafe.freeMemory(base);

        ByteBuffer x = ByteBuffer.allocateDirect(1);
        x.put((byte)0);

        long xbase = TReflect.getFieldValue(x, "address");
        unsafe.reallocateMemory(xbase,x.capacity()*2);
        xbase = TReflect.getFieldValue(x, "address");
        TReflect.setFieldValue(x,"capacity", x.capacity()*2);
        int xLimit = TReflect.getFieldValue(x, "limit");
        TReflect.setFieldValue(x,"limit", xLimit*2);
        Logger.simple(x.capacity());
        x.put((byte)1);
        x.flip();
        Logger.simple(x.get());
        Logger.simple(x.get());
    }

    public static Unsafe getUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe)field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
