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
 */
public class FastThreadLocal<T> {
	private ThreadLocal<FastThreadLocal> jdkThreadLocal = new ThreadLocal<FastThreadLocal>();
	private static AtomicInteger indexGenerator = new AtomicInteger(0);

	volatile int index = -1;

	private Object value;

	private Supplier supplier;

	/**
	 * 构造函数
	 * 		为当前对象生成一个 id
	 */
	public FastThreadLocal(){
		//分配一个索引在所有线程中都是用这个索引位置
		this.index = indexGenerator.getAndIncrement();
	}

	/**
	 * 基于提供起的过早函数
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
		tryCreate();
		if(FastThread.getThread() != null) {
			FastThreadLocal fastThreadLocal = FastThread.getThread().data[index];

			T t = (T) fastThreadLocal.value;
			if (t == null && supplier != null) {
				t = (T) supplier.get();
				fastThreadLocal.value = t;
			}

			return t;
		} else {
			FastThreadLocal fastThreadLocal = jdkThreadLocal.get();
			T t = (T) fastThreadLocal.value;
			if (t == null && supplier != null) {
				t = (T) supplier.get();
				fastThreadLocal.value = t;
			}

			return t;
		}
	}

	/**
	 * 根据线程的类型尝试创建不同的线程局部变量
	 */
	public void tryCreate(){
		if(FastThread.getThread() != null) {
			FastThreadLocal[] data = FastThread.getThread().data;
			FastThreadLocal fastThreadLocal = data[index];
			if (fastThreadLocal == null) {
				fastThreadLocal = new FastThreadLocal();
				data[index] = fastThreadLocal;
			}
		} else {
			FastThreadLocal fastThreadLocal = jdkThreadLocal.get();
			if(fastThreadLocal == null){
				jdkThreadLocal.set(new FastThreadLocal());
			}
		}
	}

	/**
	 * 设置线程局部变量
	 * @param t 线程局部变量
	 */
	public void set(T t){
		tryCreate();

		if(FastThread.getThread() != null) {
			FastThread.getThread().data[index].value = t;
		} else {
			jdkThreadLocal.get().value = t;
		}
	}

}
