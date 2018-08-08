package org.voovan.tools.cache;

import org.voovan.tools.hashwheeltimer.HashWheelTimer;

import java.util.concurrent.TimeoutException;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public abstract class Bucket {
    public static HashWheelTimer BUCKET_HASH_WHEEL_TIMER = new HashWheelTimer(1000, 1);
    static {
        BUCKET_HASH_WHEEL_TIMER.rotate();
    }

    public abstract boolean acquire();
    public abstract void acquire(int timeout) throws TimeoutException;
}
