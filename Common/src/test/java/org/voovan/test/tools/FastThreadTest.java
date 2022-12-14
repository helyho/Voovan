package org.voovan.test.tools;

import org.voovan.Global;
import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.TEnv;

/**
 * Class name
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class FastThreadTest {
    public static void main(String[] args) throws Exception {

        for(int i=0;i<24;i++) {
            int finalI = i;
            TEnv.sleep(i);
            Global.getThreadPool().execute(new mmmmm());
        }

        TEnv.sleep(1000);

        TEnv.sleep(11111);
    }


    public static class mmmmm implements Runnable{
        static FastThreadLocal m = FastThreadLocal.withInitial(()-> {
            System.out.println("===> create "+ Thread.currentThread().getName());
            return Thread.currentThread().getName() + " " +12313;
        });

        @Override
        public void run() {
            System.out.println(m.get());
            System.out.println(m.get());
        }
    }
}
