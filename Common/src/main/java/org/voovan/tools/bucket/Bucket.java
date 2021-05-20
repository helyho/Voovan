package org.voovan.tools.bucket;

import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;

import java.util.concurrent.TimeoutException;

/**
 * 令牌桶,漏桶基类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public abstract class Bucket {
    public static HashWheelTimer BUCKET_HASH_WHEEL_TIMER = new HashWheelTimer("Bucket", 1000, 1000);
    protected HashWheelTask hashWheelTask= null;
    static {
        BUCKET_HASH_WHEEL_TIMER.rotate();
    }

    public HashWheelTask getHashWheelTask() {
        return hashWheelTask;
    }

    public void setHashWheelTask(HashWheelTask hashWheelTask) {
        this.hashWheelTask = hashWheelTask;
    }

    public void release(){
        hashWheelTask.cancel();
    }

    public abstract boolean acquire();
    public abstract void acquire(int timeout) throws TimeoutException;
}
