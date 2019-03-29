package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.collection.MultiMap;
import org.voovan.tools.TEnv;


/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class MulitMapUnit extends TestCase {

    public void testMulitThreads(){
        MultiMap m = new MultiMap();
        String key = "key";
        for(int i=0;i<1000;i++){
            int mi = i;
            Global.getThreadPool().execute(()->{
                m.putValue(key, mi);
            });
        }

        for(int i=5;i<1000;i++){
            int mi = i;
            Global.getThreadPool().execute(()->{
                m.removeValue(key, 1);
            });
        }


        while(true) {
            TEnv.sleep(1);
        }
    }
}
