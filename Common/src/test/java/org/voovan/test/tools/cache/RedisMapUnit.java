package org.voovan.test.tools.cache;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.cache.RedisMap;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisMapUnit extends TestCase{

    private RedisMap redisMapOld;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        redisMapOld = new RedisMap("127.0.0.1", 6379, 2000, 100, "test", null);
    }

    public void testPut(){
        Object value = redisMapOld.put("name", "helyho");
        assertEquals(1, redisMapOld.size());
    }

    public void testGet(){
        Object value = (String) redisMapOld.get("name");
        assertEquals("helyho", value);
    }

    public void testContainsKey(){
        assertTrue(redisMapOld.containsKey("name"));
    }

    public void testRemove(){
        assertEquals("helyho", redisMapOld.remove("name"));
    }

    public void testPutAll(){
        redisMapOld.putAll(TObject.asMap("age", 35, "sexType", "male"));
        assertEquals(2, redisMapOld.size());
    }

    public void testKeySet(){
        assertEquals(2, redisMapOld.keySet().size());
    }

    public void testValues(){
        assertEquals(2, redisMapOld.values().size());
    }

    public void testIncr(){
        redisMapOld.put("incr", 12);
        assertEquals(23, redisMapOld.incr("incr", 11));
    }

    public void testIncrFloat(){
        redisMapOld.put("incrFloat", "12");
        assertEquals(23.23, redisMapOld.incrFloat("incrFloat", 11.23));
    }

    public void testClear(){
        redisMapOld.clear();
        assertEquals(0, redisMapOld.size());
    }

    public void testObject(){
        ScriptEntity scriptEntity = new ScriptEntity();
        scriptEntity.setSourcePath("sourcePath");
        scriptEntity.setPackagePath("packagePath");
        redisMapOld.put("scriptEntity", scriptEntity);
        scriptEntity = (ScriptEntity)redisMapOld.get("scriptEntity");
        Logger.simple(JSON.toJSON(scriptEntity));
    }

    public void testLazyLoad(){
        redisMapOld.supplier((key) -> {
            return key+"_loaded";
        });
        Object o = redisMapOld.get("nullValue");
        assertEquals(o, "nullValue_loaded");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        redisMapOld.close();
    }

    public void testLockTest(){
        RedisMap redisMap = new RedisMap();
        redisMap.clear();

        for(int i=0;i<1000;i++) {
            final int t = i;
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    if(t%100==0){
                        redisMap.remove("test");
                        System.out.println("remove-->" + t);
                    }
                    System.out.println(redisMap.putIfAbsent("test", "value"));
                }
            });
        }

        TEnv.sleep(60*1000);
    }
}
