package org.voovan.tools.collection;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * 线程对象池
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ThreadObjectPool<T> {
    private final ThreadLocal<RingBuffer<T>> THREAD_LOCAL_POOL =  ThreadLocal.withInitial(()->new RingBuffer<T>(2048));

    private int threadLocalMaxSize = 4;

    public ThreadObjectPool() {
    }

    public ThreadObjectPool(int threadLocalMaxSize) {
        this.threadLocalMaxSize = threadLocalMaxSize;
    }

    public int getThreadLocalMaxSize() {
        return threadLocalMaxSize;
    }

    public void setThreadLocalMaxSize(int threadLocalMaxSize) {
        this.threadLocalMaxSize = threadLocalMaxSize;
    }

    public RingBuffer<T> getThreadLoaclPool(){
        return THREAD_LOCAL_POOL.get();
    }

    public T get(Supplier<T> supplier){
        RingBuffer<T> threadLocalPool = getThreadLoaclPool();

        T t = threadLocalPool.pop();

        //创建一个新的 t
        if(t==null) {
            t = (T) supplier.get();
        }

        return t;
    }

    public void release(T t, Supplier destory){
        RingBuffer<T> threadLocalPool = getThreadLoaclPool();

        //如果小于线程中池的大小则放入线程中的池
        if(threadLocalPool.avaliable() < threadLocalMaxSize) {
            threadLocalPool.push(t);
        } else {
            destory.get();
        }
    }
}
