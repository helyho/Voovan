package org.voovan.tools;

/**
 * 快速线程(基于能够快速访问线程局部变量)
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class FastThread extends Thread {

	//每个线程持有 ThreadLocal 的数量
	public static int FAST_THREAD_LOCAL_SIZE = TEnv.getSystemProperty("FastThreadLocalSize", 4096);

	static {
		System.out.println("[FRAMEWRORK] FAST_THREAD_LOCAL_SIZE: " + FAST_THREAD_LOCAL_SIZE);
	}

	//用于保存当前线程的 FastThreadLocal
	protected FastThreadLocal[] data = new FastThreadLocal[FAST_THREAD_LOCAL_SIZE];

	public FastThread(Runnable target, String name) {
		this(null, target, name);
	}

	public FastThread(ThreadGroup group, Runnable target, String name) {
		this(group, target, name, 0);
	}

	public FastThread(ThreadGroup group, Runnable target, String name, long stackSize) {
		super(group, target, name, stackSize);
	}

	public FastThread(Runnable target) {
		super(target);
	}

	public static FastThread getThread() {
		Thread currentThread = Thread.currentThread();
		if(currentThread instanceof FastThread) {
			return (FastThread) currentThread;
		} else {
			return null;
		}
	}
}
