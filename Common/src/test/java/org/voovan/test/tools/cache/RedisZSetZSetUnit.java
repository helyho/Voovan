package org.voovan.test.tools.cache;

import org.voovan.tools.cache.RedisZSetZSet;
import org.voovan.tools.json.JSON;
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
public class RedisZSetZSetUnit extends TestCase {
    private RedisZSetZSet redisZsetZSet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisZsetZSet = new RedisZSetZSet("127.0.0.1", 6379, 2000, 100, "ZSetZSet", null);
    }

    public void testAdd(){
        Object value = redisZsetZSet.add(9.16, 11d, "heffff");
        System.out.println(value);
        assertEquals(1, redisZsetZSet.size(9.16));
    }

    public void testAddAll(){
        Map<String, Double> testMap = new HashMap<String, Double>();
        testMap.put("aaa", new Double(12));
        testMap.put("bbb", new Double(13));
        testMap.put("ccc", new Double(14));
        Object value = redisZsetZSet.addAll(9.16, testMap);
        System.out.println(value);
        assertEquals(4, redisZsetZSet.size(9.16));
    }

    public void testIncrease(){
        Object value = redisZsetZSet.increase(9.16, "heffff", 11d);
        System.out.println(value);
        assertEquals(22.0D, value);
    }

    public void testScoreRanageCount(){
        Object value = redisZsetZSet.scoreRangeCount(9.16, 12, 14);
        System.out.println(value);
        assertEquals(4, redisZsetZSet.size(9.16));
    }

    public void testValueRanageCount(){
        Object value = redisZsetZSet.valueRangeCount(9.16, "[a", "[c");
        System.out.println(value);
        assertEquals(4, redisZsetZSet.size(9.16));
    }

    public void testRangeByIndex(){
        Object value = redisZsetZSet.getRangeByIndex(9.16, 0,9);
        System.out.println(value);

        value = redisZsetZSet.getRevRangeByIndex(9.16, 0,1);
        System.out.println(value);
    }

    public void testRangeByValue(){
        Object value = redisZsetZSet.getRangeByValue(9.16,"[a", "+");
        System.out.println(value);

        value = redisZsetZSet.getRevRangeByValue(9.16,"+", "[a");
        System.out.println(value);

        value = redisZsetZSet.getRangeByValue(9.16,"[a", "+", 2, 1);
        System.out.println(value);

        value = redisZsetZSet.getRevRangeByValue(9.16,"+", "[a", 2, 1);
        System.out.println(value);
    }

    public void testRangeByScore(){
        Object value = redisZsetZSet.getRangeByScore(9.16,12, 14);
        System.out.println(value);

        value = redisZsetZSet.getRevRangeByScore(9.16,14, 12);
        System.out.println(value);

        value = redisZsetZSet.getRangeByScore(9.16,12, 14, 1, 1);
        System.out.println(value);

        value = redisZsetZSet.getRevRangeByScore(9.16,14, 12, 1, 1);
        System.out.println(value);
    }

    public void testIndexOf(){
        Object value = redisZsetZSet.indexOf(9.16, "bbb");
        System.out.println(value);
        value = redisZsetZSet.revIndexOf(9.16,"bbb");
        System.out.println(value);
    }

    public void testRemove(){
        Object value = redisZsetZSet.remove(9.16,"bbb");
        System.out.println(value);

        value = redisZsetZSet.removeRangeByValue(9.16,"[aaa", "[ccc");
        System.out.println(value);

        value = redisZsetZSet.removeRangeByIndex(9.16,1,1);
        System.out.println(value);

        value = redisZsetZSet.removeRangeByScore(9.16,13, 15);
        System.out.println(value);
    }

    public void testScore(){
        Object value = redisZsetZSet.getScore(9.16,"heffff");
        System.out.println(value);
    }

    public void testScan(){
        Object value = redisZsetZSet.scan(9.16, "99", "*", 1);
        System.out.println(JSON.toJSON(value));
    }
}
