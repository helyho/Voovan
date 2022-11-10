package org.voovan.tools.collection;

import org.voovan.tools.FastThreadLocal;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 线程对象池
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ThreadObjectPool<T> {
    private FastThreadLocal<RingBuffer<T>> threadLocalPool;

    private int threadPoolSize = 64;

    public ThreadObjectPool() {
    }

    public ThreadObjectPool(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        threadLocalPool =  FastThreadLocal.withInitial(()->new RingBuffer<T>(this.threadPoolSize));
    }


    public ThreadObjectPool(int threadPoolSize, Supplier<T> supplier) {
        this.threadPoolSize = threadPoolSize;
        threadLocalPool =  FastThreadLocal.withInitial(()->{
            RingBuffer<T> ringBuffer = new RingBuffer<T>(this.threadPoolSize);
            for(int i = 0; i< threadPoolSize; i++) {
                ringBuffer.push(supplier.get());
            }
            return ringBuffer;
        });
    }


    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    public RingBuffer<T> getPool(){
        return threadLocalPool.get();
    }

    public T get(Supplier<T> supplier){
        RingBuffer<T> pool = getPool();

        T t = pool.pop();

        //创建一个新的 t
        if(t==null) {
            t = (T) supplier.get();
        }

        return t;
    }

    public void release(T t, Consumer<T> destory){
        RingBuffer<T> pool = getPool();

        //如果小于线程中池的大小则放入线程中的池
        if(!pool.push(t)) {
            if(destory!=null) {
                destory.accept(t);
            }
        }
    }

    public void release(T t){
        release(t, null);
    }
}
