package org.voovan.test.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.cache.RedisLock;
import org.voovan.tools.cache.RedisMap;
import junit.framework.TestCase;

import java.util.ArrayList;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisLockUnit extends TestCase{

    private RedisLock redisLock;

    public volatile static int x = 0;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisLock = new RedisLock("127.0.0.1", 6379, 2000, 100, "DisLock_1", null);
    }

    public void testLockTest(){
        RedisMap redisMap = new RedisMap();
        redisMap.clear();

        ArrayList jj = new ArrayList();

        for(int i=0;i<200;i++) {
            final int t = i;
            int finalI = i;
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    redisLock.lock(1000*60, 500);
                    System.out.println(Long.toString(System.nanoTime())+ "\tlock");
                    int lock = (int)Math.random()*300;
                    TEnv.sleep(lock);
                    x++;
                    System.out.println(Long.toString(System.nanoTime())+ "\tunlock");
                    redisLock.unLock();

                }
            });
        }

        int xx = 60*1000;
        while(xx>0){
            TEnv.sleep(1);
            xx--;
        }
    }
}
