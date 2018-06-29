package org.voovan.test.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.cache.RedisLock;
import org.voovan.tools.cache.RedisMap;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

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

    Semaphore semaphore = new Semaphore(1);

    public volatile static int x = 0;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisLock = new RedisLock("127.0.0.1", 6379, 2000, 100, "DisLock", null);
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
                    while(!redisLock.lock(1000*60, 5000)){
                        TEnv.sleep(1);
                    }
                    boolean locked = semaphore.tryAcquire();
                    System.out.println(Long.toString(System.nanoTime())+ "\tlock " + x + " " + locked);
                    int lock = (int)Math.random()*300;
                    TEnv.sleep(lock);
                    x++;
                    if(locked) {
                        semaphore.release();
                    }
                    System.out.println(Long.toString(System.nanoTime())+ "\tunlock " + x);
                    while(!redisLock.unLock()){
                        TEnv.sleep(1);
                    }

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
