package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.collection.CacheMap;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author helyho
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
                .expire(1)
                .supplier((t)-> t + "_"+ System.currentTimeMillis())
                .autoRemove(true)
                .create();

        for(int i=0;i<300;i++) {
            cacheMap.put("key_" + i, "value_" + i);
        }

        TEnv.sleep(2000);

        assert cacheMap.size() == 100;
        cacheMap.clear();


        for(int x=0;x<100;x++) {
            int finalX = x;
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    ThreadLocalRandom random = ThreadLocalRandom.current();

                    cacheMap.get("key/"+ finalX);
                }
            });
        }


        TEnv.sleep(500);
        assertEquals(100, cacheMap.size());
        cacheMap.expire(0);
        cacheMap.put("key_aaa", "value_aaa");
        cacheMap.put("key_bbb", "value_bbb");
        cacheMap.put("key_ccc", "value_ccc");

        cacheMap.putIfAbsent("key_aaa", "value_111");
        cacheMap.putIfAbsent("key_bbb", "value_222");
        cacheMap.putIfAbsent("key_ccc", "value_333");


        assertEquals("value_aaa", cacheMap.get("key_aaa"));
        cacheMap.remove("key_ccc");

        int count = 0;
        while(count<140*1000) {
            TEnv.sleep(1);
            count++;
        }
    }

    public void testGetExpire(){
        CacheMap cacheMap = new CacheMap().interval(0).expire(1).create();
        cacheMap.put("aaa", "bbb");
        TEnv.sleep(1000);
        System.out.println(cacheMap.get("aaa"));

        System.out.println(cacheMap.get("ccc"));
        TEnv.sleep(60*1000);
    }

    public void testLockTest(){
        CacheMap cacheMap = new CacheMap().create().expire(5);
        for(int i=0;i<10;i++) {
            final int fi = i;

            long cn =System.nanoTime();
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    for(int x=0; x<1; x++) {
                        String value = fi + "_" + x;
                        System.out.println((System.nanoTime()-cn) + "\t current: " +  value + "\t ret: " + cacheMap.put("test", value) + "\t get: " + cacheMap.get("test"));

                    }
                }
            });
        }

        TEnv.sleep(1000);

        TEnv.sleep(60*1000);
    }

    public void testSuppler(){
        CacheMap cacheMap = new CacheMap().autoRemove(true).create();

        final AtomicInteger x = new AtomicInteger(0);

        for(int i=0;i<100;i++) {
            int finali = i;
            Object m = cacheMap.get("test" + finali, (key) -> System.currentTimeMillis() , 1l);
        }

        System.out.println("before: " + cacheMap.size());
        TEnv.sleep(2000);
        System.out.println("after: " + cacheMap.size());
        assertEquals(0, cacheMap.size());

        for(int i=10;i<20;i++) {
            Object m = cacheMap.get("test_" + i, (key) -> {
                String result = System.currentTimeMillis() + " " + key;
                return result;
            }, 5l);

            System.out.println(x.getAndIncrement() + " " + m);
        }


        TEnv.sleep(7000);
        System.out.println("after: " + cacheMap.size());
        assertEquals(0, cacheMap.size());


        for (int i=0;i<60*1000;i++){
            TEnv.sleep(500);
        }

    }


    public void testSupplerM1() {
        CacheMap cacheMap = new CacheMap().autoRemove(true).expire(1).supplier(key -> key + " " + TDateTime.now()).create();

        for(int x = 0; x<10000000;x++) {
            for (int i = 0; i < 100; i++) {
                Object obj = cacheMap.get("key" + i);
                if(obj == null) {
                    System.out.println("key" + i + " -> " + obj);
                }
            }
        }

    }

 }
