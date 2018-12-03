package org.voovan.test.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.cache.CachedHashMap;
import junit.framework.TestCase;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class CachedHashMapTest extends TestCase{

    public void testput() {
        CachedHashMap cachedHashMap = new CachedHashMap();
        cachedHashMap.put("aaa", "aaa");

        TEnv.sleep(2000);

        System.out.println(cachedHashMap.get("aaa"));
    }

    public void testBasic() {
        CachedHashMap cachedHashMap = new CachedHashMap()
                .maxSize(100)
                .interval(1)
                .expire(1)
                .supplier((t)-> t + "_"+ System.currentTimeMillis())
                .autoRemove(true)
                .create();

        for(int i=0;i<100;i++) {
            cachedHashMap.put("key_" + i, "value_" + i);
        }

        TEnv.sleep(2000);

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
        TEnv.sleep(2000);
        cachedHashMap.expire(0);
        cachedHashMap.put("key_aaa", "value_aaa");
        cachedHashMap.put("key_bbb", "value_bbb");
        cachedHashMap.put("key_ccc", "value_ccc");

        int count = 0;
        while(count<140*1000) {
            TEnv.sleep(1);
            count++;
        }
    }

    public void testLockTest(){
        CachedHashMap cachedHashMap = new CachedHashMap().create().expire(1);

        for(int i=0;i<10;i++) {
            final int fi = i;
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    for(int x=0; x<500; x++) {
                        TEnv.sleep(10);
                        System.out.println(fi + " " + x + " " + cachedHashMap.putIfAbsent("test", "value" + fi+"_" +x));
                    }
                }
            });
        }

        TEnv.sleep(60*1000);
    }

    public void testSuppler(){
        CachedHashMap cachedHashMap = new CachedHashMap().autoRemove(true).create();

        final AtomicInteger x = new AtomicInteger(0);

        for(int i=0;i<100;i++) {
            int finali = i;
            Object m = cachedHashMap.get("test" + finali, (key) -> {
                String result = System.currentTimeMillis() + " " + key;
                TEnv.sleep(1);
                System.out.println("gen "+result);
                return result;
            }, 1l);
        }

        System.out.println("before: " + cachedHashMap.size());
        TEnv.sleep(2000);
        System.out.println("after: " + cachedHashMap.size());

        for(int i=0;i<10;i++) {
            Object m = cachedHashMap.get("test", (key) -> {
                String result = System.currentTimeMillis() + " " + key;
                TEnv.sleep(1000);
                System.out.println("gen "+result);
                return result;
            }, 5l);

            System.out.println(x.getAndIncrement() + " " + m);
        }


        for (int i=0;i<60*1000;i++){
            cachedHashMap.getAndRefresh("test");
            TEnv.sleep(1);
        }

    }
}
