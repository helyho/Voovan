package org.voovan.tools;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ThreadLocalPool<T> {
    private final ConcurrentLinkedQueue<T> GLOBAL_POOL = new ConcurrentLinkedQueue<T>();
    private final ThreadLocal<LinkedList<WeakReference<T>>> THREAD_LOCAL_BYTE_BUFFER_POOL = new ThreadLocal<LinkedList<WeakReference<T>>>();

    private int globalMaxSize = 1000;
    private int threadLocalMaxSize = 10;

    public ThreadLocalPool(int globalMaxSize, int threadLocalMaxSize) {
        this.threadLocalMaxSize = threadLocalMaxSize;
        this.globalMaxSize = globalMaxSize;
    }

    public int getThreadLocalMaxSize() {
        return threadLocalMaxSize;
    }

    public void setThreadLocalMaxSize(int threadLocalMaxSize) {
        this.threadLocalMaxSize = threadLocalMaxSize;
    }

    public int getGlobalMaxSize() {
        return globalMaxSize;
    }

    public void setGlobalMaxSize(int globalMaxSize) {
        this.globalMaxSize = globalMaxSize;
    }

    public LinkedList<WeakReference<T>> getThreadLoaclPool(){
        LinkedList<WeakReference<T>> threadLocalPool = THREAD_LOCAL_BYTE_BUFFER_POOL.get();
        if(threadLocalPool == null){
            threadLocalPool = new LinkedList<WeakReference<T>>();
            THREAD_LOCAL_BYTE_BUFFER_POOL.set(threadLocalPool);
        }
        return threadLocalPool;
    }

    public T getObject(Supplier<T> supplier){
        LinkedList<WeakReference<T>> threadLocalPool = getThreadLoaclPool();
        WeakReference<T> localWeakRef = threadLocalPool.poll();

        T t = null;

        //从线程中的池中取 t
        if(localWeakRef!=null) {
            t = localWeakRef.get();
            localWeakRef.clear();
        }

        //从全局的池中取 t
        if(t==null){
            t = GLOBAL_POOL.poll();
        }

        //创建一个新的 t
        if(t==null) {
            t = (T) supplier.get();
        }

        return t;
    }

    public void release(T t, Supplier destory){
        LinkedList<WeakReference<T>> threadLocalPool = getThreadLoaclPool();
        //如果小于线程中池的大小则放入线程中的池
        if(threadLocalPool.size() < threadLocalMaxSize) {
            threadLocalPool.offer(new WeakReference<T>(t));
        }
        //如果小于全局池的大小, 则放入全局池中
        else if(GLOBAL_POOL.size() < globalMaxSize){
            GLOBAL_POOL.offer(t);
        } else {
            destory.get();
        }
    }
}
