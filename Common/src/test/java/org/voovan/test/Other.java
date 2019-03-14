package org.voovan.test;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;
import sun.misc.Unsafe;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

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
