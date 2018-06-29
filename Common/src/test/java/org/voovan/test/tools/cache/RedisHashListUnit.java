package org.voovan.test.tools.cache;

import org.voovan.tools.cache.RedisHashList;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import junit.framework.TestCase;

import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisHashListUnit extends TestCase{

    private RedisHashList redisHashList;

    private String itemName = "hwl";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisHashList = new RedisHashList("127.0.0.1", 6379, 2000, 100, "test_list", null);
    }

    public void testAdd(){
        redisHashList.add(itemName, "message 0");
        assertEquals(1, redisHashList.size());
    }

    public void testAddFirst(){
        redisHashList.addFirst(itemName, "message -1");
        assertEquals(2, redisHashList.size());
    }

    public void testGet(){
        String value = (String) redisHashList.get(itemName, 0);
        assertEquals("message -1", value);
    }

    public void testPoll(){
        String value = (String) redisHashList.poll(itemName);
        assertEquals("message 0", value);
        redisHashList.poll(itemName);
    }

    public void testPop(){
        redisHashList.add(itemName, "message 0");
        redisHashList.add(itemName, "message 1");
        redisHashList.add(itemName, "message 2");
        redisHashList.add(itemName, "message 3");

        String value = (String) redisHashList.pop(itemName);
        assertEquals("message 0", value);
    }

    public void testPeakLeast(){
        String value = (String) redisHashList.peekLast(itemName);
        assertEquals("message 3", value);
    }

    public void testPeakFirst(){
        String value = (String) redisHashList.peekFirst(itemName);
        assertEquals("message 1", value);
    }

    public void testPollFirst(){
        String value = (String) redisHashList.pollFirst(itemName);
        assertEquals("message 1", value);
    }

    public void testPollLast(){
        String value = (String) redisHashList.pollLast(itemName);
        assertEquals("message 3", value);
    }

    public void testTrim(){
        redisHashList.add(itemName, "message 0");
        redisHashList.add(itemName, "message 1");
        redisHashList.add(itemName, "message 2");
        redisHashList.add(itemName, "message 3");

        List<String> value = redisHashList.range(itemName, 1,2);
        Logger.simple(JSON.toJSON(value));
        redisHashList.trim(itemName,1,2);
    }

    public void testSet(){
        redisHashList.set(itemName, 0, " new value");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        redisHashList.close();
    }
}
