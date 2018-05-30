package org.voovan.test;

import org.voovan.Global;
import org.voovan.tools.Memory;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Other {

    public static void main(String[] args) throws IOException {
        Memory memory = new Memory(1024*1024);
        Logger.info("start");
        System.out.println(memory);

        long address1 = memory.allocate(128);
        System.out.println(address1);

        TEnv.sleep(10);

        AtomicInteger in = new AtomicInteger(0);
        for(int i=0;i<100;i++) {
            int x = i;
            Global.getThreadPool().execute(()->{
                long address = memory.allocate((long) (1024));
                TEnv.sleep((int) (Math.random()*5));
                if(x%10 <5)
                memory.release(address);
                in.incrementAndGet();
                System.out.println(address);
            });
        }


        Logger.info("end");

        while (true){
            TEnv.sleep(1000);
            System.out.println(in.get());
        }
    }
}
