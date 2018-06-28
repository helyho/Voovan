package org.voovan.test.tools.cache;

import org.voovan.tools.cache.RedisHashsWithList;
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
public class RedisHashsWithListUnit extends TestCase{

    private RedisHashsWithList redisHashsList;

    private String itemName = "hwl";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisHashsList = new RedisHashsWithList("127.0.0.1", 6379, 2000, 100, "test_list", null);
    }

    public void testAdd(){
        redisHashsList.add(itemName, "message 0");
        assertEquals(1, redisHashsList.size());
    }

    public void testAddFirst(){
        redisHashsList.addFirst(itemName, "message -1");
        assertEquals(2, redisHashsList.size());
    }

    public void testGet(){
        String value = (String)redisHashsList.get(itemName, 0);
        assertEquals("message -1", value);
    }

    public void testPoll(){
        String value = (String)redisHashsList.poll(itemName);
        assertEquals("message 0", value);
        redisHashsList.poll(itemName);
    }

    public void testPop(){
        redisHashsList.add(itemName, "message 0");
        redisHashsList.add(itemName, "message 1");
        redisHashsList.add(itemName, "message 2");
        redisHashsList.add(itemName, "message 3");

        String value = (String)redisHashsList.pop(itemName);
        assertEquals("message 0", value);
    }

    public void testPeakLeast(){
        String value = (String)redisHashsList.peekLast(itemName);
        assertEquals("message 3", value);
    }

    public void testPeakFirst(){
        String value = (String)redisHashsList.peekFirst(itemName);
        assertEquals("message 1", value);
    }

    public void testPollFirst(){
        String value = (String)redisHashsList.pollFirst(itemName);
        assertEquals("message 1", value);
    }

    public void testPollLast(){
        String value = (String)redisHashsList.pollLast(itemName);
        assertEquals("message 3", value);
    }

    public void testTrim(){
        redisHashsList.add(itemName, "message 0");
        redisHashsList.add(itemName, "message 1");
        redisHashsList.add(itemName, "message 2");
        redisHashsList.add(itemName, "message 3");

        List<String> value = redisHashsList.range(itemName, 1,2);
        Logger.simple(JSON.toJSON(value));
        redisHashsList.trim(itemName,1,2);
    }

    public void testSet(){
        redisHashsList.set(itemName, 0, " new value");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        redisHashsList.close();
    }
}
