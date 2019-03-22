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

        String mm = null;

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<50000000;i++) {
                if(mm == null){
                    continue;
                }
            }
        })/1000000);

        String qq = "aaa";

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<50000000;i++) {
                if(qq != null){
                    continue;
                }
            }
        })/1000000);


        boolean oo = true;

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<50000000;i++) {
                if(oo){
                    continue;
                }
            }
        })/1000000);

        boolean gg = true;

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<50000000;i++) {
                if(!gg){
                    continue;
                }
            }
        })/1000000);
    }



}
