package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.tools.collection.RedisMapWithZSet;
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
public class RedisMapWithZSetUnit extends TestCase {
    private RedisMapWithZSet redisHashsWithSortedSet;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisHashsWithSortedSet = new RedisMapWithZSet("127.0.0.1", 6379, 2000, 100, "HashZSet", null);
    }

    public void testAdd(){
        Object value = redisHashsWithSortedSet.add("sn", 11d, "heffff");
        System.out.println(value);
        assertEquals(1, redisHashsWithSortedSet.size("sn"));
    }

    public void testAddAll(){
        Map<String, Double> testMap = new HashMap<String, Double>();
        testMap.put("aaa", new Double(12));
        testMap.put("bbb", new Double(13));
        testMap.put("ccc", new Double(14));
        Object value = redisHashsWithSortedSet.addAll("sn", testMap);
        System.out.println(value);
        assertEquals(4, redisHashsWithSortedSet.size("sn"));
    }

    public void testIncrease(){
        Object value = redisHashsWithSortedSet.increase("sn", "heffff", 11d);
        System.out.println(value);
        assertEquals(1, redisHashsWithSortedSet.size());
    }

    public void testScoreRanageCount(){
        Object value = redisHashsWithSortedSet.scoreRangeCount("sn", 12, 14);
        System.out.println(value);
        assertEquals(1, redisHashsWithSortedSet.size());
    }

    public void testValueRanageCount(){
        Object value = redisHashsWithSortedSet.valueRangeCount("sn", "[a", "[c");
        System.out.println(value);
        assertEquals(1, redisHashsWithSortedSet.size());
    }

    public void testRangeByIndex(){
        Object value = redisHashsWithSortedSet.getRangeByIndex("sn", 0,9);
        System.out.println(value);

        value = redisHashsWithSortedSet.getRevRangeByIndex("sn", 0,1);
        System.out.println(value);
    }

    public void testRangeByValue(){
        Object value = redisHashsWithSortedSet.getRangeByValue("sn","[a", "+");
        System.out.println(value);

        value = redisHashsWithSortedSet.getRevRangeByValue("sn","+", "[a");
        System.out.println(value);

        value = redisHashsWithSortedSet.getRangeByValue("sn","[a", "+", 2, 1);
        System.out.println(value);

        value = redisHashsWithSortedSet.getRevRangeByValue("sn","+", "[a", 2, 1);
        System.out.println(value);
    }

    public void testRangeByScore(){
        Object value = redisHashsWithSortedSet.getRangeByScore("sn",12, 14);
        System.out.println(value);

        value = redisHashsWithSortedSet.getRevRangeByScore("sn",14, 12);
        System.out.println(value);

        value = redisHashsWithSortedSet.getRangeByScore("sn",12, 14, 1, 1);
        System.out.println(value);

        value = redisHashsWithSortedSet.getRevRangeByScore("sn",14, 12, 1, 1);
        System.out.println(value);
    }

    public void testIndexOf(){
        Object value = redisHashsWithSortedSet.indexOf("sn", "bbb");
        System.out.println(value);
        value = redisHashsWithSortedSet.revIndexOf("sn","bbb");
        System.out.println(value);
    }

    public void testRemove(){
        Object value = redisHashsWithSortedSet.remove("sn","bbb");
        System.out.println(value);

        value = redisHashsWithSortedSet.removeRangeByValue("sn","[aaa", "[ccc");
        System.out.println(value);

        value = redisHashsWithSortedSet.removeRangeByIndex("sn",1,1);
        System.out.println(value);

        value = redisHashsWithSortedSet.removeRangeByScore("sn",13, 15);
        System.out.println(value);
    }

    public void testScore(){
        Object value = redisHashsWithSortedSet.getScore("sn","heffff");
        System.out.println(value);
    }

    public void testScan(){
        Object value = redisHashsWithSortedSet.scan("sn", "99", "*", 1);
        System.out.println(JSON.toJSON(value));
    }
}
