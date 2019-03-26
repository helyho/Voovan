package org.voovan.test;

import org.voovan.tools.TEnv;
import org.voovan.tools.reflect.TReflect;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
        ArrayBlockingQueue arrayBlockingQueue = new ArrayBlockingQueue(50000000);
        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000;i++) {
                arrayBlockingQueue.add(i);
            }
        }));


        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000;i++) {
                arrayBlockingQueue.poll();
            }
        }));


        PriorityQueue priorityQueue = new PriorityQueue();
        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000;i++) {
                priorityQueue.add(i);
            }
        }));


        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000;i++) {
                priorityQueue.poll();
            }
        }));


        LinkedBlockingQueue linkedBlockingQueue = new LinkedBlockingQueue();
        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000;i++) {
                linkedBlockingQueue.add(i);
            }
        }));


        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000;i++) {
                linkedBlockingQueue.poll();
            }
        }));
    }



}
