package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.FastThread;
import org.voovan.tools.TEnv;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class FastThreadLocalUnit extends TestCase {
	public static FastThreadLocal XX = FastThreadLocal.withInitial(()->Long.valueOf(System.currentTimeMillis()).toString());
	public static FastThreadLocal YY = FastThreadLocal.withInitial(()->Long.valueOf(System.currentTimeMillis()).toString() + "YY");

	public static class test implements Runnable {
		public void run(){
			int i = 0;
			while (true) {
				TEnv.sleep(1000);
				if(i!=0 && i%4==0){
					XX.set(XX.getSupplier().get());
				}
				i++;

				Object a =  XX.get();
				Object b =  YY.get();
				System.out.println("Thread: " + Thread.currentThread().getId() + ", Xvalue:" + a + " " + XX.get().hashCode() + ", Yvalue:" + b + " " + YY.get().hashCode());
			}
		}
	}

	public void test(){
		for (int i=0;i<2;i++){
			FastThread thread = new FastThread(new test());
			thread.start();
			TEnv.sleep(500);
		}

		TEnv.sleep(300000);
	}
}
