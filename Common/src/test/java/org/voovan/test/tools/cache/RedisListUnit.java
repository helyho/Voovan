package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.tools.cache.RedisList;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

import java.util.List;
import java.util.ListIterator;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisListUnit extends TestCase{

    private RedisList redisList;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisList = new RedisList("10.0.0.101", 6379, 2000, 100, "test_list", null);
    }

    public void testAdd(){
        redisList.add("message 0");
        assertEquals(1, redisList.size());
    }

    public void testAddFirst(){
        redisList.addFirst("message -1");
        assertEquals(2, redisList.size());
    }

    public void testGet(){
        String value = (String)redisList.get(0);
        assertEquals("message -1", value);
    }

    public void testPoll(){
        String value = (String)redisList.poll();
        assertEquals("message 0", value);
        redisList.poll();
    }

    public void testPop(){
        redisList.add("message 0");
        redisList.add("message 1");
        redisList.add("message 2");
        redisList.add("message 3");

        String value = (String)redisList.pop();
        assertEquals("message 0", value);
    }

    public void testPeakLeast(){
        String value = (String)redisList.peekLast();
        assertEquals("message 3", value);
    }

    public void testPeakFirst(){
        String value = (String)redisList.peekFirst();
        assertEquals("message 1", value);
    }

    public void testPollFirst(){
        String value = (String)redisList.pollFirst();
        assertEquals("message 1", value);
    }

    public void testPollLast(){
        String value = (String)redisList.pollLast();
        assertEquals("message 3", value);
    }

    public void testTrim(){
        redisList.add("message 0");
        redisList.add("message 1");
        redisList.add("message 2");
        redisList.add("message 3");

        List<String> value = redisList.range(1,2);
        Logger.simple(JSON.toJSON(value));
        redisList.trim(1,2);
    }

    public void testSet(){
        redisList.set(0, " new value");
    }

    public void testIterator(){
        ListIterator<String> iter = redisList.listIterator();
        Logger.simple(iter.hasNext());
        Logger.simple(iter.hasPrevious());
        Logger.simple(iter.next());
        Logger.simple(iter.hasNext());
        Logger.simple(iter.hasPrevious());
        Logger.simple(iter.previous());
    }



    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        redisList.close();
    }
}
