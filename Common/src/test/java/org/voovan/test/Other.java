package org.voovan.test;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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

    public static void main(String[] args) throws IOException {
        TreeMap<BigDecimal, Object> test = new TreeMap<BigDecimal, Object>(new Comparator<BigDecimal>() {
            @Override
            public int compare(BigDecimal o1, BigDecimal o2) {
                return o2.compareTo(o1);
            }
        });
        test.put(new BigDecimal(1), 123);
        test.put(new BigDecimal(2), 123);
        test.put(new BigDecimal(3), 123);
        test.put(new BigDecimal(4), 123);
        test.put(new BigDecimal(5), 123);
        test.put(new BigDecimal(6), 123);



        System.out.println(test);

        System.out.println(test.subMap(new BigDecimal(4), new BigDecimal(1)));
        System.out.println(JSON.toJSON(test.keySet().toArray(new BigDecimal[0])));

        TreeMap<BigDecimal, Object> test1 = test;

        System.out.println(JSON.toJSON(test1.keySet().toArray(new BigDecimal[0])));
        System.out.println(test1);

        test = new TreeMap<BigDecimal, Object>();
        test.put(new BigDecimal(1), 123);
        test.put(new BigDecimal(2), 123);
        test.put(new BigDecimal(3), 123);
        test.put(new BigDecimal(4), 123);
        test.put(new BigDecimal(5), 123);
        test.put(new BigDecimal(6), 123);

        System.out.println(test);

        System.out.println(test.subMap(new BigDecimal(1), new BigDecimal(4)));

        System.out.println(JSON.toJSON(test.keySet().toArray(new BigDecimal[0])));


    }
}
