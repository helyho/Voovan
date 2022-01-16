package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.collection.CacheMap;

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
public class CacheMapTest extends TestCase{

    public void testput() {
        CacheMap cacheMap = new CacheMap();
        cacheMap.put("aaa", "aaa");

        TEnv.sleep(2000);

        System.out.println(cacheMap.get("aaa"));
    }

    public void testBasic() {
        CacheMap cacheMap = new CacheMap()
                .maxSize(100)
                .interval(1)
//                .expire(1000)
                .supplier((t)-> t + "_"+ System.currentTimeMillis())
                .autoRemove(true)
                .create();

        for(int i=0;i<300;i++) {
            cacheMap.put("key_" + i, "value_" + i);
        }

        TEnv.sleep(2000);

        for(int x=0;x<10003;x++) {
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    int index = random.nextInt(0, 100);
                    cacheMap.get("key_"+index);
                }
            });
        }
        TEnv.sleep(500);
        TEnv.sleep(1500);
        cacheMap.expire(0);
        cacheMap.put("key_aaa", "value_aaa");
        cacheMap.put("key_bbb", "value_bbb");
        cacheMap.put("key_ccc", "value_ccc");

        int count = 0;
        while(count<140*1000) {
            TEnv.sleep(1);
            count++;
        }
    }

    public void testLockTest(){
        CacheMap cacheMap = new CacheMap().create().expire(1);
        for(int i=0;i<10;i++) {
            final int fi = i;
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    for(int x=0; x<500; x++) {
                        TEnv.sleep(10);
                        System.out.println(fi + " " + x + " " + cacheMap.putIfAbsent("test", "value" + fi+"_" +x));
                    }
                }
            });
        }

        TEnv.sleep(60*1000);
    }

    public void testSuppler(){
        CacheMap cacheMap = new CacheMap().autoRemove(true).create();

        final AtomicInteger x = new AtomicInteger(0);

        for(int i=0;i<100;i++) {
            int finali = i;
            Object m = cacheMap.get("test" + finali, (key) -> {
                String result = System.currentTimeMillis() + " " + key;
                TEnv.sleep(1);
                System.out.println("gen "+result);
                return result;
            }, 1l);
        }

        System.out.println("before: " + cacheMap.size());
        TEnv.sleep(2000);
        System.out.println("after: " + cacheMap.size());

        for(int i=0;i<10;i++) {
            Object m = cacheMap.get("test", (key) -> {
                String result = System.currentTimeMillis() + " " + key;
                TEnv.sleep(1000);
                System.out.println("gen "+result);
                return result;
            }, 5l);

            System.out.println(x.getAndIncrement() + " " + m);
        }


        for (int i=0;i<60*1000;i++){
            cacheMap.getAndRefresh("test");
            TEnv.sleep(1);
        }

    }
}
