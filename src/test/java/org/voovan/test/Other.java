package org.voovan.test;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TByteBuffer;
import org.voovan.tools.TEnv;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import sun.jvm.hotspot.utilities.Bits;
import sun.misc.Unsafe;
import sun.misc.VM;
import sun.net.www.protocol.http.HttpURLConnection;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;


public class Other {


    public static void main(String[] args) throws Exception {

        Logger.simple("Process ID: "+ TEnv.getCurrentPID());
        ByteBufferChannel bbf = new ByteBufferChannel(0);
        long address = TReflect.getFieldValue(bbf, "address");

        for(int i=0;i<1024*1024*100;i++){
            bbf.writeEnd(ByteBuffer.wrap("a".getBytes()));
        }

        Logger.simple("xxxx");

        bbf.free();


        Logger.simple("xxxx");
    }


}
