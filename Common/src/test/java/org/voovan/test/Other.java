package org.voovan.test;

import org.voovan.tools.TEnv;
import org.voovan.tools.reflect.TReflect;

import java.util.ArrayList;
import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Other {


    private static int apple = 10;
    private int orange = 10;

    public static void main(String[] args) throws Exception {

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<50000000;i++) {
               Object.class.isAssignableFrom(List.class);
                ArrayList.class.isAssignableFrom(List.class);
            }
        })/1000000);

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<50000000;i++) {
                TReflect.isExtendsByClass(ArrayList.class, Object.class);
                TReflect.isImpByInterface(ArrayList.class, List.class);
            }
        })/1000000);
    }



}
