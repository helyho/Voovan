package org.voovan.test.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.cache.ObjectCachedPool;
import org.voovan.tools.log.Logger;
import junit.framework.TestCase;

import java.util.ArrayList;

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

        ObjectCachedPool objectPool = new ObjectCachedPool(2);
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
        ObjectCachedPool objectPool = new ObjectCachedPool();

        for(int i=0;i<100;i++) {
            Object item = "element " + i;
            if(pooledId==null) {
                pooledId = objectPool.add(item);
            }else{
                objectPool.add(item);
            }
        }

        TEnv.sleep(3000);

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
}
