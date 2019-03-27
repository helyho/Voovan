package org.voovan.tools;

import java.util.LinkedList;
import java.util.function.Supplier;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ObjectThreadPool<T> {
    private final ThreadLocal<LinkedList<T>> THREAD_LOCAL_POOL =  ThreadLocal.withInitial(()->new LinkedList<T>());

    private int threadLocalMaxSize = 4;

    public ObjectThreadPool() {
    }

    public ObjectThreadPool(int threadLocalMaxSize) {
        this.threadLocalMaxSize = threadLocalMaxSize;
    }

    public int getThreadLocalMaxSize() {
        return threadLocalMaxSize;
    }

    public void setThreadLocalMaxSize(int threadLocalMaxSize) {
        this.threadLocalMaxSize = threadLocalMaxSize;
    }

    public LinkedList<T> getThreadLoaclPool(){
        return THREAD_LOCAL_POOL.get();
    }

    public T get(Supplier<T> supplier){
        LinkedList<T> threadLocalPool = getThreadLoaclPool();

        T t = threadLocalPool.poll();

        //创建一个新的 t
        if(t==null) {
            t = (T) supplier.get();
        }

        return t;
    }

    public void release(T t, Supplier destory){
        LinkedList<T> threadLocalPool = getThreadLoaclPool();

        //如果小于线程中池的大小则放入线程中的池
        if(threadLocalPool.size() < threadLocalMaxSize) {
            threadLocalPool.offer(t);
        } else {
            destory.get();
        }
    }
}
