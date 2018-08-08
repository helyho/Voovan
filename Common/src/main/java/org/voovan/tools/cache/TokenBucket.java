package org.voovan.tools.cache;

import org.voovan.tools.TEnv;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 无锁令牌桶
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class TokenBucket extends Bucket{

    private AtomicInteger atomicInteger = new AtomicInteger(0);
    private long lastVisitTime = System.currentTimeMillis();
    private int releaseTime = 10*1000;


    /**
     * 令牌桶构造函数
     * @param tokenSize 令牌桶默认大小
     * @param interval 令牌桶的新增周期, 每次触发新增一个令牌到令牌桶, 单位: 毫秒
     * @param releaseTime 令牌桶失效并自动移除的时间
     */
    public TokenBucket(int tokenSize, int interval, int releaseTime){
        init(tokenSize, interval, releaseTime);
    }

    /**
     * 令牌桶构造函数
     * @param tokenSize 令牌桶默认大小
     * @param interval 令牌桶的新增周期, 每次触发新增一个令牌到令牌桶, 单位: 毫秒
     */
    public TokenBucket(int tokenSize, int interval){
        init(tokenSize, interval, Integer.MAX_VALUE);
    }

    /**
     * 令牌桶构造函数
     * @param tokenSize 令牌桶默认大小
     * @param interval 令牌桶的新增周期, 每次触发新增一个令牌到令牌桶, 单位: 毫秒
     */
    public void init(int tokenSize, int interval, int releaseTime){

        atomicInteger.set(tokenSize);
        //刷新令牌桶的任务
        Bucket.BUCKET_HASH_WHEEL_TIMER.addTask(new HashWheelTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastVisitTime >= releaseTime) {
                    this.cancel();
                } else {
                    atomicInteger.getAndUpdate((val) -> {
                        if (val >= tokenSize) {
                            return tokenSize;
                        } else {
                            return val + tokenSize;
                        }
                    });
                }
            }
        }, interval, true);
    }

    /**
     * 获取令牌, 立即返回
     * @return true: 拿到令牌, false: 没有拿到令牌
     */
    public boolean acquire() {
        lastVisitTime = System.currentTimeMillis();
        int value = atomicInteger.getAndUpdate((val) -> {
            if(val <= 0){
                return 0;
            } else {
                return val-1;
            }
        });

        return value > 0 ;
    }


    /**
     * 获取令牌, 带有时间等待
     * @param timeout 等待时间
     * @throws TimeoutException 超时异常
     */
    public void acquire(int timeout) throws TimeoutException {
        lastVisitTime = System.currentTimeMillis();
        TEnv.wait(timeout, ()->!acquire());
    }
}
