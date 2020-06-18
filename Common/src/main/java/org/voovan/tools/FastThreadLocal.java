package org.voovan.tools;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 线程局部变量
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 *
 * 每个 FastThreadLocal 在各个线程中都会持有一个 index 索引位置,实际访问的时候访问的是这个 FastThreadLocal 对象在各个线程中保存的那个实例
 */
public class FastThreadLocal<T> {
    private ThreadLocal<FastThreadLocal> jdkThreadLocal = new ThreadLocal<FastThreadLocal>();
    private static AtomicInteger indexGenerator = new AtomicInteger(0);

	private int index = -1;

	private Object value;

	private Supplier supplier;

    /**
     * 构造函数
     * 		为当前对象生成一个 id
     */
    public FastThreadLocal(){
        this(indexGenerator.getAndIncrement());
        System.out.println(index);
    }

    /**
     * 构造函数
     * 		为当前对象生成一个 id
     */
    private FastThreadLocal(int index){
        this.index =  index;
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
     * 获取 线程局部变量
     * @return 线程局部变量
     */
    public T get() {
        FastThreadLocal fastThreadLocal = tryCreate();

        T t = (T) fastThreadLocal.value;
        if (t == null && supplier != null) {
            t = (T) supplier.get();
            fastThreadLocal.value = t;
        }

        return t;

    }

    /**
     * 根据线程的类型尝试创建不同的线程局部变量
     * @return true: FastThreadLocal, false: JdkThreadLocal
     */
    private FastThreadLocal tryCreate(){
        FastThread thread = FastThread.getThread();

        if(thread != null && index < FastThread.FAST_THREAD_LOCAL_SIZE) {
            FastThreadLocal[] data = thread.data;
            FastThreadLocal fastThreadLocal = data[index];
            if (fastThreadLocal == null) {
                fastThreadLocal = new FastThreadLocal(this.index);
                data[index] = fastThreadLocal;
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
     * 设置线程局部变量
     * @param t 线程局部变量
     */
    public void set(T t){
        FastThreadLocal fastThreadLocal = tryCreate();
        fastThreadLocal.value = t;
    }

}

