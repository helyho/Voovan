package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.ObjectPool;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class ObjectPoolUnit extends TestCase {


    public void testAdd(){
        String pooledId = null;

        ObjectPool objectPool = new ObjectPool(2);
        for(int i=0;i<30;i++) {
            String item = "element " + i;
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
}
