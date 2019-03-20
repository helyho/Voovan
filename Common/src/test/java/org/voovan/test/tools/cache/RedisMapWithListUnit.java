package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.tools.cache.RedisMapWithList;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisMapWithListUnit extends TestCase{

    private RedisMapWithList redisMapWithList;

    private String itemName = "hwl";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisMapWithList = new RedisMapWithList("127.0.0.1", 6379, 2000, 100, "HashList", null);
    }

    public void testAdd(){
        redisMapWithList.add(itemName, "message 0");
        assertEquals(1, redisMapWithList.size());
    }

    public void testAddFirst(){
        redisMapWithList.addFirst(itemName, "message -1");
        assertEquals(1, redisMapWithList.size());
    }

    public void testGet(){
        String value = (String) redisMapWithList.get(itemName, 0);
        assertEquals("message 0", value);
    }

    public void testPoll(){
        String value = (String) redisMapWithList.poll(itemName);
        assertEquals(" new value", value);
        redisMapWithList.poll(itemName);
    }

    public void testPop(){
        redisMapWithList.add(itemName, "message 0");
        redisMapWithList.add(itemName, "message 1");
        redisMapWithList.add(itemName, "message 2");
        redisMapWithList.add(itemName, "message 3");

        String value = (String) redisMapWithList.pop(itemName);
        assertEquals("message 0", value);
    }

    public void testPeakLeast(){
        String value = (String) redisMapWithList.peekLast(itemName);
        assertEquals("message 3", value);
    }

    public void testPeakFirst(){
        String value = (String) redisMapWithList.peekFirst(itemName);
        assertEquals("message 2", value);
    }

    public void testPollFirst(){
        String value = (String) redisMapWithList.pollFirst(itemName);
        assertEquals("message -1", value);
    }

    public void testPollLast(){
        String value = (String) redisMapWithList.pollLast(itemName);
        assertEquals("message 2", value);
    }

    public void testTrim(){
        redisMapWithList.add(itemName, "message 0");
        redisMapWithList.add(itemName, "message 1");
        redisMapWithList.add(itemName, "message 2");
        redisMapWithList.add(itemName, "message 3");

        List<String> value = redisMapWithList.range(itemName, 1,2);
        Logger.simple(JSON.toJSON(value));
        redisMapWithList.trim(itemName,1,2);
    }

    public void testSet(){
        redisMapWithList.set(itemName, 0, " new value");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        redisMapWithList.close();
    }
}
