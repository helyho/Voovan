package org.voovan.test;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import sun.jvm.hotspot.utilities.Bits;
import sun.misc.Unsafe;
import sun.misc.VM;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;


public class Other {


    public static void main(String[] args) throws Exception {
        Properties p = System.getProperties();
        for(Map.Entry e : p.entrySet()){
            Logger.simple(e.getKey()+"="+ e.getValue());
        }
    }


}
