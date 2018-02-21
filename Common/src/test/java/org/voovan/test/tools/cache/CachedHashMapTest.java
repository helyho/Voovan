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

    public void testBasic() {
        CachedHashMap cachedHashMap = CachedHashMap.newInstance()
                .maxSize(100)
                .interval(1)
                .supplier((t)-> t + "_"+ System.currentTimeMillis())
                .create();

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
        TEnv.sleep(2000);
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
        CachedHashMap cachedHashMap = CachedHashMap.newInstance().create();

        for(int i=0;i<10;i++) {
            final int fi = i;
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    for(int x=0; x<500; x++) {
                        System.out.println(fi + " " + x + " " + cachedHashMap.putIfAbsent("test", "value"));
                    }
                }
            });
        }

        TEnv.sleep(60*1000);
    }

    public void testSuppler(){
        CachedHashMap cachedHashMap = CachedHashMap.newInstance().autoRemove(false).create();

        final AtomicInteger x = new AtomicInteger(0);

        for(int i=0;i<100;i++) {
            Global.getThreadPool().execute(() -> {
                Object m = cachedHashMap.get("test", (key) -> {
                    String result = System.currentTimeMillis() + " " + key;
                    TEnv.sleep(1000);
                    cachedHashMap.expire(key, 3000);
                    System.out.println("gen "+result);
                    return result;
                });

                System.out.println(x.getAndIncrement() + " " + m);
            });
        }

        TEnv.sleep(3000);

        for(int i=0;i<10;i++) {
            Global.getThreadPool().execute(() -> {
                Object m = cachedHashMap.get("test", (key) -> {
                    String result = System.currentTimeMillis() + " " + key;
                    TEnv.sleep(1000);
                    cachedHashMap.expire(key, 3000);
                    System.out.println("gen "+result);
                    return result;
                });

                System.out.println(x.getAndIncrement() + " " + m);
            });
        }


        TEnv.sleep(60*1000);




    }
}
