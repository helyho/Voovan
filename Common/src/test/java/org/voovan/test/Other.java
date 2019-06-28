package org.voovan.test;

import org.voovan.tools.TEnv;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.reflect.TReflect;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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

        HashMap<Integer, Integer> hashMap = new HashMap<>();
        ConcurrentHashMap<Integer, Integer> treeMap = new ConcurrentHashMap<>();

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++)
            hashMap.put(i, i);
        }));

        System.out.println(TEnv.measureTime(()->{
            for(int i=0;i<1000000;i++)
                treeMap.put(i, i);
        }));

        System.out.println(TEnv.measureTime(()->{
            Iterator<Map.Entry<Integer, Integer>> iterator = hashMap.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry entry = iterator.next();
                entry.getValue();
                entry.getKey();
                hashMap.size();
            }
        }));

        System.out.println(TEnv.measureTime(()->{
            Iterator<Map.Entry<Integer, Integer>> iterator = treeMap.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry entry = iterator.next();
                entry.getValue();
                entry.getKey();
                treeMap.size();
            }
        }));


        System.out.println("1-"+ "12344321".hashCode());
        System.out.println("2-"+ "43211234".hashCode());
    }
}
