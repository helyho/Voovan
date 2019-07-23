package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.bucket.TokenBucket;

import java.util.concurrent.TimeoutException;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TokenBucketUnit extends TestCase {

    public void testNormal(){
        TokenBucket tokenBucket = new TokenBucket(1, 1000);

        System.out.println(tokenBucket.acquire());
        System.out.println(tokenBucket.acquire());
        TEnv.sleep(1000);
        System.out.println(tokenBucket.acquire());
        System.out.println(tokenBucket.acquire());

        TEnv.sleep(3000);
        System.out.println("================================================");

        try {
            tokenBucket.acquire(1000);
            System.out.println(true);
        } catch (TimeoutException e) {
            System.out.println(false);
        }
        try {
            tokenBucket.acquire(500);
            System.out.println(true);
        } catch (TimeoutException e) {
            System.out.println(false);
        }
    }

    public void testParaller(){
        final TokenBucket tokenBucket = new TokenBucket(5, 1000);
        for(int i=0; i <20; i++) {
            Global.getThreadPool().execute(() -> {
                TEnv.sleep((int) (Math.random() * 1000));
                try {
                    tokenBucket.acquire(1000);
                    System.out.println(true);
                } catch (TimeoutException e) {
                    System.out.println(false);
                }
            });
        }

        TEnv.sleep(1000*10);
    }
}
