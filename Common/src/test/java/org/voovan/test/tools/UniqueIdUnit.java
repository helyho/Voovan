package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.UniqueId;
import org.voovan.tools.log.Logger;

import javax.sound.midi.Soundbank;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class UniqueIdUnit extends TestCase {

    public void testUniqueId(){
        ThreadPoolExecutor threadPoolExecutor = Global.getThreadPool();
        UniqueId uniqueId = new UniqueId();
        System.out.println("--start--");
        System.out.println(System.currentTimeMillis());
        for(int i=0;i<100;i++) {
            try {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String value = "";
                        for (int k = 0; k < 100; k++) {
                            System.out.println(uniqueId.nextId());
                        }
                        System.out.println(100 + " "+System.currentTimeMillis());
                    }
                });
            }catch(RejectedExecutionException e){
                TEnv.sleep(1);
                i--;
            }
        }

        TEnv.sleep(10000);
        System.out.println("--end--");
    }
}
