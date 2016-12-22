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
public class ObjectPoolUtil extends TestCase {


    public void testAdd(){
        int hashCode = 0;

        ObjectPool objectPool = new ObjectPool(1);
        for(int i=0;i<30;i++) {
            String item = "element " + i;
            if(hashCode==0) {
                hashCode = objectPool.add(item);
            }else{
                objectPool.add(item);
            }
        }
        for(int m=0;m<30;m++) {
            objectPool.get(hashCode);
            TEnv.sleep(100);
        }
        assertEquals(1,objectPool.size());
        assertEquals(0, objectPool.add(null));
    }
}
