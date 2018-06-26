package org.voovan.test.tools.cache;

import org.voovan.tools.cache.RedisSortedSet;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RedisSortedSetUnit extends TestCase {
    private RedisSortedSet redisSortedSet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisSortedSet = new RedisSortedSet("127.0.0.1", 6379, 2000, 100, "zsetTest", null);
    }

    public void testAdd(){
        Object value = redisSortedSet.add(11d, "heffff");
        System.out.println(value);
        assertEquals(1, redisSortedSet.size());
    }

    public void testAddAll(){
        Map<String, Double> testMap = new HashMap<String, Double>();
        testMap.put("aaa", new Double(12));
        testMap.put("bbb", new Double(13));
        testMap.put("ccc", new Double(14));
        Object value = redisSortedSet.addAll(testMap);
        System.out.println(value);
        assertEquals(1, redisSortedSet.size());
    }

    public void testIncrease(){
        Object value = redisSortedSet.increase("helyho3", 11d);
        System.out.println(value);
        assertEquals(1, redisSortedSet.size());
    }

    public void testScoreRanageCount(){
        Object value = redisSortedSet.scoreRangeCount(12, 14);
        System.out.println(value);
        assertEquals(1, redisSortedSet.size());
    }

    public void testValueRanageCount(){
        Object value = redisSortedSet.valueRangeCount("[a", "[c");
        System.out.println(value);
        assertEquals(1, redisSortedSet.size());
    }

    public void testRangeByIndex(){
        Object value = redisSortedSet.rangeByIndex(0,1);
        System.out.println(value);

        value = redisSortedSet.revRangeByIndex(0,1);
        System.out.println(value);
    }

    public void testRangeByValue(){
        Object value = redisSortedSet.rangeByValue("[a", "+");
        System.out.println(value);

        value = redisSortedSet.revRangeByValue("+", "[a");
        System.out.println(value);

        value = redisSortedSet.rangeByValue("[a", "+", 2, 1);
        System.out.println(value);

        value = redisSortedSet.revRangeByValue("+", "[a", 2, 1);
        System.out.println(value);
    }

    public void testRangeByScore(){
        Object value = redisSortedSet.rangeByScore(12, 14);
        System.out.println(value);

        value = redisSortedSet.revRangeByScore(14, 12);
        System.out.println(value);

        value = redisSortedSet.rangeByScore(12, 14, 1, 1);
        System.out.println(value);

        value = redisSortedSet.revRangeByScore(14, 12, 1, 1);
        System.out.println(value);
    }

    public void testIndexOf(){
        Object value = redisSortedSet.indexOf("bbb");
        System.out.println(value);
        value = redisSortedSet.revIndexOf("bbb");
        System.out.println(value);
    }

    public void testRemove(){
        Object value = redisSortedSet.remove("bbb");
        System.out.println(value);

        value = redisSortedSet.removeRangeByValue("[aaa", "[ccc");
        System.out.println(value);

        value = redisSortedSet.removeRangeByIndex(1,1);
        System.out.println(value);

        value = redisSortedSet.removeRangeByScore(13, 15);
        System.out.println(value);
    }

    public void testScore(){
        Object value = redisSortedSet.getScore("helyho31");
        System.out.println(value);
    }

    public void testScan(){
        Object value = redisSortedSet.scan("99", "h*", 1);
        System.out.println(value);
    }
}
