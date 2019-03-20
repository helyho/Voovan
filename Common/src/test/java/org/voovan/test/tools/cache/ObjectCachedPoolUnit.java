package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.cache.ObjectPool;
import org.voovan.tools.log.Logger;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

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
        Object pooledId = null;

        ObjectPool objectPool = new ObjectPool(2);
        for(int i=0;i<30;i++) {
            Object item = "element " + i;
            if(pooledId==null) {
                pooledId = objectPool.add(item);
            }else{
                objectPool.add(item);
            }
        }
        Logger.simple(pooledId);

        for(int m=0;m<30;m++) {
            objectPool.get(pooledId);
            TEnv.sleep(100);
        }
        assertEquals(1,objectPool.size());
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

        ArrayList<Object> arrayList = new ArrayList<Object>();
        for(int i=0;i<50;i++){
            Global.getThreadPool().execute(()->{
                Object objectId = objectPool.borrow();
                arrayList.add(objectId);
                Logger.simple("borrow1->" + objectId);
            });
        }

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
                Object objectId = objectPool.borrow();
                arrayList.add(objectId);
                Logger.simple("borrow2->" +objectId);
            });

        }

        TEnv.sleep(1000);
    }

    private static Integer item = 0;
    public void testBorrowConcurrent() {

        Object pooledId = null;
        ObjectPool objectPool = new ObjectPool().minSize(3).maxSize(1000).aliveTime(5).supplier(()->{
            return item++;
        }).create();



        LinkedBlockingDeque<Object> arrayList = new LinkedBlockingDeque<Object>();

        int count = 0;

        while(true){
            Global.getThreadPool().execute(()->{

                if((int)(Math.random()*10 % 2) == 0) {
                    Object objectId = objectPool.borrow(1000);
                    if(objectId!=null) {
                        arrayList.offer(objectId);
                        Logger.simple("borrow->" + objectId);
                    } else {
                        Logger.simple("borrow failed ================");
                    }
                } else {
                    Object objectId = arrayList.poll();
                    if(objectId!=null) {
                        objectPool.restitution(objectId);
                        Logger.simple("restitution->" + objectId);
                    }
                }
            });
            count++;
            if(count >= 100000){
                break;
            }
        }

        count = count*10;

        while(count>=0) {
            count--;
            TEnv.sleep(1);
        }
    }
}
