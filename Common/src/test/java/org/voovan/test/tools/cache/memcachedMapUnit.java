package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.tools.TEnv;
import org.voovan.tools.cache.MemcachedMap;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class memcachedMapUnit extends TestCase {

    private MemcachedMap mm;

    @Override
    public void setUp() throws Exception {
        mm = new MemcachedMap();
    }


    public void testSet(){
        mm.put("test", "message");
        Logger.info(mm.get("test"));
    }

    public void testExpireSet(){
        mm.put("testExpire", "message", 2);
        TEnv.sleep(3000);
        Logger.info("=====" + mm.get("testExpire"));
    }
}
