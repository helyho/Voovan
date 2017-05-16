package org.voovan.test;

import org.voovan.tools.ByteBufferChannel;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public class Other {


    public static void main(String[] args) throws Exception {

        Logger.simple(Integer.valueOf(1123).getClass().isPrimitive());
        Logger.simple("Process ID: "+ TEnv.getCurrentPID());
        Object[][] mm = new Object[][]{{12}};

        Logger.simple(mm.getClass().getCanonicalName()+" / "+mm.getClass().getSimpleName()+
                " / "+mm.getClass().getName() + " / " +mm.getClass().getTypeName()+" / "+mm.getClass().toGenericString());

    }
}
