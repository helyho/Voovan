package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.collection.CachedMap;

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
public class CachedMapTest extends TestCase{

    public void testput() {
        CachedMap cachedMap = new CachedMap();
        cachedMap.put("aaa", "aaa");

        TEnv.sleep(2000);

        System.out.println(cachedMap.get("aaa"));
    }

    public void testBasic() {
        CachedMap cachedMap = new CachedMap()
                .maxSize(100)
                .interval(1)
                .expire(1)
                .supplier((t)-> t + "_"+ System.currentTimeMillis())
                .autoRemove(true)
                .create();

        for(int i=0;i<100;i++) {
            cachedMap.put("key_" + i, "value_" + i);
        }

        TEnv.sleep(2000);

        for(int x=0;x<10003;x++) {
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    int index = random.nextInt(0, 100);
                    cachedMap.get("key_"+index);
                }
            });
        }
        TEnv.sleep(500);
        TEnv.sleep(1500);
        cachedMap.expire(0);
        cachedMap.put("key_aaa", "value_aaa");
        cachedMap.put("key_bbb", "value_bbb");
        cachedMap.put("key_ccc", "value_ccc");

        int count = 0;
        while(count<140*1000) {
            TEnv.sleep(1);
            count++;
        }
    }

    public void testLockTest(){
        CachedMap cachedMap = new CachedMap().create().expire(1);
        for(int i=0;i<10;i++) {
            final int fi = i;
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    for(int x=0; x<500; x++) {
                        TEnv.sleep(10);
                        System.out.println(fi + " " + x + " " + cachedMap.putIfAbsent("test", "value" + fi+"_" +x));
                    }
                }
            });
        }

        TEnv.sleep(60*1000);
    }

    public void testSuppler(){
        CachedMap cachedMap = new CachedMap().autoRemove(true).create();

        final AtomicInteger x = new AtomicInteger(0);

        for(int i=0;i<100;i++) {
            int finali = i;
            Object m = cachedMap.get("test" + finali, (key) -> {
                String result = System.currentTimeMillis() + " " + key;
                TEnv.sleep(1);
                System.out.println("gen "+result);
                return result;
            }, 1l);
        }

        System.out.println("before: " + cachedMap.size());
        TEnv.sleep(2000);
        System.out.println("after: " + cachedMap.size());

        for(int i=0;i<10;i++) {
            Object m = cachedMap.get("test", (key) -> {
                String result = System.currentTimeMillis() + " " + key;
                TEnv.sleep(1000);
                System.out.println("gen "+result);
                return result;
            }, 5l);

            System.out.println(x.getAndIncrement() + " " + m);
        }


        for (int i=0;i<60*1000;i++){
            cachedMap.getAndRefresh("test");
            TEnv.sleep(1);
        }

    }
}
