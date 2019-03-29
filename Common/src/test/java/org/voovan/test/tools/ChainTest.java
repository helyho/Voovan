package org.voovan.test.tools;

import org.voovan.Global;
import org.voovan.tools.collection.Chain;
import org.voovan.tools.TEnv;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ChainTest {
    public static void main(String[] args) {
        Chain<Integer> a = new Chain<Integer>();
        a.add(0);
        a.add(1);
        a.add(2);
        a.add(3);
        a.add(4);
        a.add(5);
        a.add(6);
        a.add(7);

        for(int i=0;i<500;i++) {
            Global.getThreadPool().execute(()->{
                a.rewind();
                String m = /*Thread.currentThread().getName() + " " +*/ a.iteratorLocal.get() + "/" + a.invertedIteratorLocal.get() + " = ";
                while (a.hasPrevious()) {
                    m = m + a.previous() + ", ";
                }
                System.out.println(m);
            });
        }

        TEnv.sleep(1000);

        for(int i=0;i<500;i++) {
            Global.getThreadPool().execute(()->{
                a.rewind();
                String m = /*Thread.currentThread().getName() + " " +*/ a.iteratorLocal.get() + "/" + a.invertedIteratorLocal.get() + " = ";
                while (a.hasNext()) {
                    m = m + a.next() + ", ";
                }
                System.out.println(m);
            });
        }
    }
}
