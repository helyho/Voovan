package org.voovan.test.tools;

import org.voovan.tools.TEnv;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.log.Logger;
import sun.misc.Unsafe;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UnsafePerformance {


    public static void main(String[] args) throws Exception {
        Logger.simple(TEnv.getCurrentPID());
        long address = 0;
        long start = 0;

        //切换这个标记,使用堆和堆外内存,观察性能情况
        boolean useStackMemory = true;

        for(int x=0;x<10;x++) {
            start = System.currentTimeMillis();
            for (int i = 0; i < 100000; i++) {
                if(useStackMemory) {
                    byte[] b = new byte[1024 * 50];
                } else {
                    address = TUnsafe.getUnsafe().allocateMemory(1024 * 50);
                    TUnsafe.getUnsafe().freeMemory(address);
                }
            }
            Logger.simple("=="+(System.currentTimeMillis() - start)+"==");
        }
    }
}
