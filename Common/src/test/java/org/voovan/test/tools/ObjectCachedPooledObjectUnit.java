package org.voovan.test.tools;

import junit.framework.TestCase;
import org.junit.Before;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.exception.WeaveException;
import org.voovan.tools.json.JSON;
import org.voovan.tools.weave.Weave;
import org.voovan.tools.weave.WeaveConfig;
import org.voovan.tools.pool.ObjectPool;
import org.voovan.tools.pool.annotation.PooledObject;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public class ObjectCachedPooledObjectUnit extends TestCase {

    public void testMaxBorrow() {
        TestPoolObject t = new TestPoolObject("element ");

        ObjectPool objectPool = new ObjectPool(2).interval(1).destory(k-> {
            System.out.println("destory: " + JSON.toJSON(k));
            return null;
        }).maxBorrow(10).create();


        objectPool.add(t);

        for(int k=0;k<30;k++) {
            Global.async(()->{
                for (int i = 0; i < 50; i++) {
                    TestPoolObject t1 = (TestPoolObject) objectPool.borrow();
                    System.out.println(Thread.currentThread().getName() + " " + i + " " + t1);
                    objectPool.restitution(t1);
                    TEnv.sleep(100);
                }
            });
        }

        while(true) {
            TEnv.sleep(100);
        }
    }

    public void testLongTimeBorrow() {
        TestPoolObject t = new TestPoolObject("element ");

        ObjectPool objectPool = new ObjectPool(2).interval(1).create();
        objectPool.add(t);

        TestPoolObject t1 = (TestPoolObject) objectPool.borrow();

        TEnv.sleep(100000);
    }

    public void testAddAndLiveTime(){
        TestPoolObject t = new TestPoolObject("element ");
        ObjectPool objectPool = new ObjectPool(2).interval(1).create();
        for(int i=0;i<5;i++) {
            TestPoolObject item = new TestPoolObject("element " + i);
            objectPool.add(item);
        }

        TEnv.sleep(4000);

        assertEquals(0,objectPool.size());
    }

    public void testBorrow() {
        Object pooledId = null;
        ObjectPool<TestPoolObject> objectPool = new ObjectPool();

        for(int i=0;i<50;i++) {
            TestPoolObject item = new TestPoolObject("element " + i);
            if(pooledId==null) {
                pooledId = objectPool.add(item);
            } else {
                objectPool.add(item);
            }
        }

        TEnv.sleep(1000);

        ArrayList<TestPoolObject> arrayList = new ArrayList<TestPoolObject>();
        for(int i=0;i<50;i++){
            Global.async(()->{
                TestPoolObject objectId = objectPool.borrow();
                arrayList.add(objectId);
                System.out.println("borrow1->" + objectId);
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

        for(int i=0;i<100;i++){
            Global.async(()->{
                TestPoolObject objectId = objectPool.borrow();
                arrayList.add(objectId);
                System.out.println("borrow2->" +objectId);
            });

        }

        TEnv.sleep(1000);
    }

    private static Integer item = 0;
    public void testBorrowConcurrent() {

        Object pooledId = null;
        ObjectPool<TestPoolObject> objectPool = new ObjectPool().minSize(1).maxSize(1).aliveTime(500000).supplier(()->{
            return  new TestPoolObject("element " + item++);
        }).create();



        ConcurrentLinkedQueue<TestPoolObject> queue = new ConcurrentLinkedQueue<TestPoolObject>();

        AtomicInteger count = new AtomicInteger(0);

        for(int i=0;i<100;i++){
            Thread t = new Thread(()->{
                while (count.incrementAndGet() < 50000) {
//                    if ((int) (Math.random() * 10 % 2) == 0) {
                        TestPoolObject object = null;
                        try {
                            object = objectPool.borrow(1000);
                        } catch (TimeoutException e) {
                            e.printStackTrace();
                        }

                        if (object != null) {
//                            queue.offer(object);
                            System.out.println("borrow->" + object);
                        } else {
                            System.out.println("borrow failed ================");
                        }
//                    } else {
//                        TestPoolObject object = queue.poll();
                        if (object != null) {
                            System.out.println("restitution->" + object);
                            objectPool.restitution(object);
                        }
//                    }
                }
            });

            t.start();
        }



        while(count.incrementAndGet() < 1000) {
            TEnv.sleep(1);
        }

        while(queue.size() >0){
            objectPool.restitution(queue.poll());
        }
    }

    @PooledObject
    public class TestPoolObject extends org.voovan.tools.pool.PooledObject {
        private String k;

        public TestPoolObject(String k) {
            this.k = k;
        }

        public String getK() {
            return k;
        }

        public void setK(String k) {
            this.k = k;
        }

        @Override
        public String toString() {
            return "testPoolObject{" +
                    "k='" + k + '\'' +
                    '}';
        }
    }
}
