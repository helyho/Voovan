package org.voovan.test;

import org.voovan.tools.Memory;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.io.IOException;
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
        Memory memory = new Memory(1024*1024*1024*2L);
        Logger.info("start");
        System.out.println(memory);

        long address = memory.allocate(1024*1024);
        System.out.println(address);

        TEnv.sleep(10);

        for(int i=0;i<100;i++) {
            address = memory.allocate(1024 * 1024);
            if(i%10<9){
                memory.release(address);
            }
            System.out.println(address);
        }


        Logger.info("end");

        while (true){
            TEnv.sleep(1);
        }
    }
}
