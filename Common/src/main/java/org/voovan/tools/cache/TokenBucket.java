package org.voovan.tools.cache;

import org.voovan.tools.TEnv;

import java.util.Timer;
import java.util.TimerTask;
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
public class TokenBucket {

    private AtomicInteger atomicInteger = new AtomicInteger(0);

    /**
     * 令牌桶构造函数
     * @param tokenSize 令牌桶默认大小
     * @param interval 令牌桶的新增周期, 每次触发新增一个令牌到令牌桶, 单位: 毫秒
     */
    public TokenBucket(int tokenSize, int interval){

        atomicInteger.set(tokenSize);
        //刷新令牌桶的任务
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                atomicInteger.getAndUpdate((val)->{
                    if(val >= tokenSize){
                        return tokenSize;
                    } else {
                        return val+1;
                    }
                });
            }
        }, interval, interval);
    }

    /**
     * 获取令牌, 立即返回
     * @return true: 拿到令牌, false: 没有拿到令牌
     */
    public boolean acquire() {
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
        TEnv.wait(timeout, ()->!acquire());
    }
}
