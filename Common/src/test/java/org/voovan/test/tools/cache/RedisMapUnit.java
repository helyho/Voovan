package org.voovan.test.tools.cache;

import org.voovan.tools.TObject;
import org.voovan.tools.cache.RedisMap;
import junit.framework.TestCase;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisMapUnit extends TestCase{

    private RedisMap redisMap;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisMap = new RedisMap("10.0.0.101", 6379, 2000, 100, "test", null);
    }

    public void testPut(){
        String value = redisMap.put("name", "helyho");
        assertEquals(1, redisMap.size());
    }

    public void testGet(){
        String value = (String)redisMap.get("name");
        assertEquals("helyho", value);
    }

    public void testContainsKey(){
        assertTrue(redisMap.containsKey("name"));
    }

    public void testRemove(){
        assertEquals("helyho", redisMap.remove("name"));
    }

    public void testPutAll(){
        redisMap.putAll(TObject.asMap("age", "35", "sexType", "male"));
        assertEquals(2, redisMap.size());
    }

    public void testKeySet(){
        assertEquals(2, redisMap.keySet().size());
    }

    public void testValues(){
        assertEquals(2, redisMap.values().size());
    }

    public void testIncr(){
        redisMap.put("incr", "12");
        assertEquals(23, redisMap.incr("incr", 11));
    }

    public void testIncrFloat(){
        redisMap.put("incrFloat", "12");
        assertEquals(23.23, redisMap.incrFloat("incrFloat", 11.23));
    }

    public void testClear(){
        redisMap.clear();
        assertEquals(0, redisMap.size());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        redisMap.close();
    }
}
