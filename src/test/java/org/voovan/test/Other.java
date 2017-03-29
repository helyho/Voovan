package org.voovan.test;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.nio.ByteBuffer;


public class Other {


    public static void main(String[] args) throws Exception {

        Logger.simple("Process ID: "+ TEnv.getCurrentPID());
        ByteBufferChannel bbf = new ByteBufferChannel(0);
        long address = TReflect.getFieldValue(bbf, "address");

        for(int i=0;i<1024*1024*100;i++){
            bbf.writeEnd(ByteBuffer.wrap("a".getBytes()));
        }

        Logger.simple("xxxx");

        bbf.release();


        Logger.simple("xxxx");
    }


}
