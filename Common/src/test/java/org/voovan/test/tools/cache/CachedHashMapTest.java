package org.voovan.test.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.cache.CachedHashMap;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class CachedHashMapTest {

    public static void main(String[] args) {
        CachedHashMap cachedHashMap = new CachedHashMap<>(100);

        for(int i=0;i<100;i++) {
            cachedHashMap.put("key_" + i, "value_" + i, Long.valueOf(5+i));
        }
        TEnv.sleep(1000);

        for(int x=0;x<10000;x++) {
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    int index = random.nextInt(0, 100);
                    cachedHashMap.get("key_"+index);
                }
            });
        }
        TEnv.sleep(1000);
        cachedHashMap.put("key_aaa", "value_aaa");
        cachedHashMap.put("key_bbb", "value_bbb");
        cachedHashMap.put("key_ccc", "value_ccc");

        int count = 0;
        while(count<140*1000) {
            TEnv.sleep(1);
            count++;
        }
    }
}
