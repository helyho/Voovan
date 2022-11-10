package org.voovan.tools;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 线程局部变量
 *      1. 声明时为作为标记索引为使用
 *      2. get / set 时作为 wrap 使用
 *      3. 如果当前索引位不够用, 则采用 java.lang.ThreadLocal 补充
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 *
 * 每个 FastThreadLocal 在各个线程中都会持有一个 index 索引位置,实际访问的时候访问的是这个 FastThreadLocal 对象在各个线程中保存的那个实例
 */
public class FastThreadLocal<T> {
    private ThreadLocal<FastThreadLocal> jdkThreadLocal = new ThreadLocal<FastThreadLocal>();
    private static AtomicInteger INDEX_GENERATOR = new AtomicInteger(0);

	private int index = -1;

	private Object value;

	private Supplier supplier;

    /**
     * 构造函数
     * 		为当前对象生成一个 id, 在各个线程的 data 数组中标记当前对象在线程中的索引位置
     */
    private FastThreadLocal(int index){
        this.index =  index;
    }

    /**
     * 构造函数
     * 		为当前对象生成一个 id
     */
    public FastThreadLocal(){
        this(INDEX_GENERATOR.getAndIncrement());
    }

    /**
     * 基于提供器的构造函数
     * @param supplier 线程局部变量生成器
     * @param <T> 范型类型
     * @return FastThreadLocal对象
     */
    public static <T> FastThreadLocal<T> withInitial(Supplier<T> supplier){
        FastThreadLocal<T> fastThreadLocal = new FastThreadLocal<T>();
        fastThreadLocal.setSupplier(supplier);
        return fastThreadLocal;
    }

    /**
     * 获取 线程局部变量生成器
     * @return  线程局部变量生成器
     */
    public Supplier<T> getSupplier() {
        return supplier;
    }

    /**
     * 设置 线程局部变量生成器
     * @param supplier 线程局部变量生成器
     */
    public void setSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }


    /**
     * 在当前线程中的 data 数组中查找可用的 ThreadLocal 对象
     * @return null: 不存在, not null: 存在
     */
    private FastThreadLocal find(){
        FastThread thread = FastThread.getThread();

        if(thread != null && index < FastThread.FAST_THREAD_LOCAL_SIZE) {
            FastThreadLocal fastThreadLocal = thread.data[index];
            if (fastThreadLocal == null) {
                fastThreadLocal = new FastThreadLocal(this.index);
                thread.data[index] = fastThreadLocal;
            }
            return fastThreadLocal;
        } else {
            FastThreadLocal fastThreadLocal = jdkThreadLocal.get();
            if(fastThreadLocal == null){
                fastThreadLocal = new FastThreadLocal(this.index);
                jdkThreadLocal.set(fastThreadLocal);
            }
            return fastThreadLocal;
        }
    }

    /**
     * 获取 线程局部变量
     * @return 线程局部变量
     */
    public T get() {
        FastThreadLocal fastThreadLocal = find();

        T t = (T) fastThreadLocal.value;
        if (t == null && supplier != null) {
            t = (T) supplier.get();
            fastThreadLocal.value = t;
        }

        return t;

    }

    /**
     * 设置线程局部变量
     * @param t 线程局部变量
     */
    public void set(T t){
        FastThreadLocal fastThreadLocal = find();
        fastThreadLocal.value = t;
    }

}

