package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.UniqueId;

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
        final UniqueId uniqueId = new UniqueId(200);
        final UniqueId uniqueId1 = new UniqueId(1985);

        System.out.println("--start--");
        System.out.println(System.currentTimeMillis());
        for(int i=0;i<500;i++) {
            try {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        String value = "";
                        for (int k = 0; k < 10000; k++) {
//                            System.out.println(uniqueId.nextStringId() + " "+System.currentTimeMillis());
                            long data = uniqueId.nextNumber();
                            long data1 = uniqueId1.nextNumber();
                            System.out.println(data + " -0- " + TString.radixConvert(data, 62));
                            System.out.println(data1 + " -1- " + TString.radixConvert(data1, 62));
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
