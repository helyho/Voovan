package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.tools.collection.RedisZSetWithZSet;
import org.voovan.tools.json.JSON;

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
public class RedisZSetWithZSetUnit extends TestCase {
    private RedisZSetWithZSet redisZsetWithZSet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisZsetWithZSet = new RedisZSetWithZSet("127.0.0.1", 6379, 2000, 100, "ZSetZSet", null);
    }

    public void testAdd(){
        Object value = redisZsetWithZSet.add(9.16, 11d, "heffff");
        System.out.println(value);
        assertEquals(1, redisZsetWithZSet.size(9.16));
    }

    public void testAddAll(){
        Map<String, Double> testMap = new HashMap<String, Double>();
        testMap.put("aaa", new Double(12));
        testMap.put("bbb", new Double(13));
        testMap.put("ccc", new Double(14));
        Object value = redisZsetWithZSet.addAll(9.16, testMap);
        System.out.println(value);
        assertEquals(4, redisZsetWithZSet.size(9.16));
    }

    public void testIncrease(){
        Object value = redisZsetWithZSet.increase(9.16, "heffff", 11d);
        System.out.println(value);
        assertEquals(22.0D, value);
    }

    public void testScoreRanageCount(){
        Object value = redisZsetWithZSet.scoreRangeCount(9.16, 12, 14);
        System.out.println(value);
        assertEquals(4, redisZsetWithZSet.size(9.16));
    }

    public void testValueRanageCount(){
        Object value = redisZsetWithZSet.valueRangeCount(9.16, "[a", "[c");
        System.out.println(value);
        assertEquals(4, redisZsetWithZSet.size(9.16));
    }

    public void testRangeByIndex(){
        Object value = redisZsetWithZSet.getRangeByIndex(9.16, 0,9);
        System.out.println(value);

        value = redisZsetWithZSet.getRevRangeByIndex(9.16, 0,1);
        System.out.println(value);
    }

    public void testRangeByValue(){
        Object value = redisZsetWithZSet.getRangeByValue(9.16,"[a", "+");
        System.out.println(value);

        value = redisZsetWithZSet.getRevRangeByValue(9.16,"+", "[a");
        System.out.println(value);

        value = redisZsetWithZSet.getRangeByValue(9.16,"[a", "+", 2, 1);
        System.out.println(value);

        value = redisZsetWithZSet.getRevRangeByValue(9.16,"+", "[a", 2, 1);
        System.out.println(value);
    }

    public void testRangeByScore(){
        Object value = redisZsetWithZSet.getRangeByScore(9.16,12, 14);
        System.out.println(value);

        value = redisZsetWithZSet.getRevRangeByScore(9.16,14, 12);
        System.out.println(value);

        value = redisZsetWithZSet.getRangeByScore(9.16,12, 14, 1, 1);
        System.out.println(value);

        value = redisZsetWithZSet.getRevRangeByScore(9.16,14, 12, 1, 1);
        System.out.println(value);
    }

    public void testIndexOf(){
        Object value = redisZsetWithZSet.indexOf(9.16, "bbb");
        System.out.println(value);
        value = redisZsetWithZSet.revIndexOf(9.16,"bbb");
        System.out.println(value);
    }

    public void testRemove(){
        Object value = redisZsetWithZSet.remove(9.16,"bbb");
        System.out.println(value);

        value = redisZsetWithZSet.removeRangeByValue(9.16,"[aaa", "[ccc");
        System.out.println(value);

        value = redisZsetWithZSet.removeRangeByIndex(9.16,1,1);
        System.out.println(value);

        value = redisZsetWithZSet.removeRangeByScore(9.16,13, 15);
        System.out.println(value);
    }

    public void testScore(){
        Object value = redisZsetWithZSet.getScore(9.16,"heffff");
        System.out.println(value);
    }

    public void testScan(){
        Object value = redisZsetWithZSet.scan(9.16, "99", "*", 1);
        System.out.println(JSON.toJSON(value));
    }
}
