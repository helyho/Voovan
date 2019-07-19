package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.collection.ObjectPool;
import org.voovan.tools.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class ObjectCachedPoolUnit extends TestCase {


    public void testAddAndLiveTime(){

        ObjectPool objectPool = new ObjectPool(2).create();
        for(int i=0;i<30;i++) {
            Object item = "element " + i;
            objectPool.add(item);
        }

        TEnv.sleep(3000);

        assertEquals(0,objectPool.size());
        assertEquals(null, objectPool.add(null));
    }

    public void testBorrow() {
        Object pooledId = null;
        ObjectPool objectPool = new ObjectPool();

        for(int i=0;i<1000;i++) {
            Object item = "element " + i;
            if(pooledId==null) {
                pooledId = objectPool.add(item);
            }else{
                objectPool.add(item);
            }
        }

        TEnv.sleep(1000);

        ArrayList<Long> arrayList = new ArrayList<Long>();
        for(int i=0;i<50;i++){
            Global.getThreadPool().execute(()->{
                Long objectId = objectPool.borrow();
                arrayList.add(objectId);
                Logger.simple("borrow1->" + objectId);
            });
        }

        TEnv.sleep(3000);

        System.out.println("===================");
        objectPool.restitution(arrayList.get(0));
        objectPool.restitution(arrayList.get(1));
        objectPool.restitution(arrayList.get(2));
        objectPool.restitution(arrayList.get(3));
        System.out.println(arrayList.get(0));
        System.out.println(arrayList.get(1));
        System.out.println(arrayList.get(2));
        System.out.println(arrayList.get(3));

        System.out.println("===================");

        for(int i=0;i<50;i++){
            Global.getThreadPool().execute(()->{
                Long objectId = objectPool.borrow();
                arrayList.add(objectId);
                Logger.simple("borrow2->" +objectId);
            });

        }

        TEnv.sleep(1000);
    }

    private static Integer item = 0;
    public void testBorrowConcurrent() {

        Object pooledId = null;
        ObjectPool objectPool = new ObjectPool().minSize(3).maxSize(50).aliveTime(5).supplier(()->{
            return item++;
        }).create();



        LinkedBlockingDeque<Long> queue = new LinkedBlockingDeque<Long>();

        AtomicInteger count = new AtomicInteger(0);

        for(int i=0;i<100;i++){
            Thread t = new Thread(()->{
                while (count.incrementAndGet() < 10000) {
                    if ((int) (Math.random() * 10 % 2) == 0) {
                        Long objectId = null;
                        try {
                            objectId = objectPool.borrow(1000);
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                        }

                        if (objectId != null) {
                            queue.offer(objectId);
                            Logger.simple("borrow->" + objectId);
                        } else {
                            Logger.simple("borrow failed ================");
                        }
                    } else {
                        Long objectId = queue.poll();
                        if (objectId != null) {
                            Logger.simple("restitution->" + objectId);
                            objectPool.restitution(objectId);
                        }
                    }
                }
            });

            t.start();
        }



        while(count.incrementAndGet() < 10000) {
            TEnv.sleep(1);
        }

        while(queue.size() >0){
            objectPool.restitution(queue.poll());
        }
    }
}
