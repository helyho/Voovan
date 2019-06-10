package org.voovan.test;

import org.voovan.tools.TEnv;
import org.voovan.tools.collection.RocksMap;
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

        RocksMap rocksMap = new RocksMap("one", "testdb");
        System.out.println("get aaaa: "+ rocksMap.get("aaaa"));
        System.out.println("get cccc: "+ rocksMap.get("cccc"));
        System.out.println("get eeee: "+ rocksMap.get("eeee"));
        System.out.println("get hhhh: "+ rocksMap.get("hhhh"));}



}
