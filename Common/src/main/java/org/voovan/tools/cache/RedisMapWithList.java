package org.voovan.tools.cache;

import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

/**
 *
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RedisMapWithList implements Closeable {
    private JedisPool redisPool;
    private String name = null;
    private int dbIndex = 0;

    private HashMap<String, String> scriptHashMap = new HashMap<String, String>();

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param name        在 redis 中的 HashMap的名称
     * @param password    redis 服务密码
     */
    public RedisMapWithList(String host, int port, int timeout, int poolsize, String name, String password){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(poolsize);
        poolConfig.setMaxTotal(poolsize);

        //如果没有指定密码,则默认不需要密码
        if(password==null) {
            redisPool = new JedisPool(poolConfig, host, port, timeout);
        }else {
            redisPool = new JedisPool(poolConfig, host, port, timeout, password);
        }
        this.name = name;
    }

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param name        在 redis 中的 HashMap的名称
     */
    public RedisMapWithList(String host, int port, int timeout, int poolsize, String name){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(poolsize);
        poolConfig.setMaxTotal(poolsize);

        redisPool = new JedisPool(poolConfig, host, port, timeout);
        this.name = name;
    }

    /**
     * 构造函数
     * @param name 在 redis 中的 HashMap的名称
     */
    public RedisMapWithList(String name){
        this.redisPool = CacheStatic.getDefaultRedisPool();
        this.name = name;
    }

    /**
     * 构造函数
     * @param jedisPool redis 连接池
     * @param name 在 redis 中的 HashMap的名称
     */
    public RedisMapWithList(JedisPool jedisPool, String name){
        this.redisPool = jedisPool;
        this.name = name;
    }

    /**
     * 构造函数
     * @param jedisPool redis 连接池
     */
    public RedisMapWithList(JedisPool jedisPool){
        this.redisPool = jedisPool;
    }

    /**
     * 获取当前选择的数据集
     * @return 数据集序号
     */
    public int getDbIndex() {
        return dbIndex;
    }

    private Jedis getJedis(){
        Jedis Jedis = redisPool.getResource();
        Jedis.select(dbIndex);
        return Jedis;
    }

    /**
     * 选择当前数据集
     * @param dbIndex 数据集序号
     */
    public void setDbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
    }

    /**
     * 执行 jedis 的脚本
     * @param jedis jedis对象
     * @param command 执行的脚本
     * @param itemName 内部 list 名称
     * @param values 值
     * @return 脚本执行返回的值
     */
    public Object eval(Jedis jedis, String command, String itemName, Object ... values){
        String params = ", ";

        String cachedKey = command+values.length;
        String scriptHash = scriptHashMap.get(cachedKey);

        for (int i = 0; i < values.length; i++) {
            if(scriptHash==null) {
                params = params + "ARGV[" + (i + 3) + "], ";
            }
            values[i] = values[i].toString();
        }

        List valueList = TObject.asList(this.name, itemName);
        valueList.addAll(TObject.asList(values));

        if(scriptHash==null){
            params = TString.removeSuffix(params.trim());

            String script = "local innerKey = redis.call('HEXISTS', ARGV[1], ARGV[2]);\n" +
                    "if (innerKey == 0) then\n" +
                    "    innerKey = tostring(ARGV[1])..'-'..tostring(ARGV[2]);\n" +
                    "    redis.call('hset', ARGV[1], innerKey, innerKey);\n" +
                    "else \n" +
                    "    innerKey = tostring(ARGV[1])..'-'..tostring(ARGV[2]);\n" +
                    "end\n" +
                    "return redis.call('" + command + "', innerKey" + params + ");";
            scriptHash =jedis.scriptLoad(script);
            scriptHashMap.put(cachedKey, scriptHash);

            Logger.fremawork("Create " + cachedKey + ": " + scriptHash);
            System.out.println(script);
        }


        return jedis.evalsha(scriptHash, Collections.emptyList(), valueList);
    }

    private ArrayList convertScoreMembersToArrays(Map<String, Double> scoreMembers) {
        ArrayList args = new ArrayList(scoreMembers.size() * 2);
        Iterator iterator = scoreMembers.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<String, Double> entry = (Map.Entry)iterator.next();
            args.add(entry.getValue().toString());
            args.add(entry.getKey());
        }

        return args;
    }

    public int size() {
        try(Jedis jedis = getJedis()) {
            return Long.valueOf(jedis.hlen(name)).intValue();
        }
    }


    public int size(String itemName) {
        try(Jedis jedis = getJedis()) {
            return (int) eval(jedis, "zcard", itemName);
        }
    }


    public String get(String itemName, int index) {
        try (Jedis jedis = getJedis()) {
            return (String)eval(jedis, "lindex", itemName, index);
        }
    }

    public String set(String itemName, int index, String element) {
        try (Jedis jedis = getJedis()) {
            return (String)eval(jedis, "lset", itemName, index, element);
        }
    }

    public boolean add(String itemName, String s) {
        return offerLast(itemName, s);
    }

    public boolean offer(String itemName, String s) {
        return offerLast(itemName, s);
    }

    public boolean offerFirst(String itemName, String s) {
        try (Jedis jedis = getJedis()) {
            eval(jedis, "lpush", itemName, s);
            return true;
        }
    }

    public boolean offerLast(String itemName, String s) {
        try (Jedis jedis = getJedis()) {
            eval(jedis, "rpush", itemName, s);
            return true;
        }
    }

    public void addFirst(String itemName, String s) {
        offerFirst(itemName, s);
    }

    public void addLast(String itemName, String s) {
        offerLast(itemName, s);
    }


    public String removeFirst(String itemName) {
        try (Jedis jedis = getJedis()) {
            return (String)eval(jedis, "lpop", itemName);
        }
    }

    public String removeLast(String itemName) {
        try (Jedis jedis = getJedis()) {
            return (String)eval(jedis, "lpop", itemName);
        }
    }

    public List<String> removeFirst(String itemName, int timeout) {
        try (Jedis jedis = getJedis()) {
            return (List<String>)eval(jedis, "blpop", itemName);
        }
    }

    public List<String> removeLast(String itemName, int timeout) {
        try (Jedis jedis = getJedis()) {
            return (List<String>)eval(jedis, "brpop", itemName);
        }
    }

    public String pollFirst(String itemName) {
        return removeFirst(itemName);
    }

    public String pollLast(String itemName) {
        return removeLast(itemName);
    }

    public String getFirst(String itemName) {
        try (Jedis jedis = getJedis()) {
            return (String)eval(jedis, "lindex", itemName, 0);

        }
    }

    public String getLast(String itemName) {
        try (Jedis jedis = getJedis()) {
            return (String)eval(jedis, "lindex", itemName, -1);
        }
    }

    public String peekFirst(String itemName) {
        return getFirst(itemName);
    }

    public String peekLast(String itemName) {
        return getLast(itemName);
    }

    public String remove(String itemName) {
        return removeFirst(itemName);
    }

    public String remove(String itemName, int index) {
        try (Jedis jedis = getJedis()) {
            String value = get(itemName, index);
            eval(jedis, "lrem", itemName, 1, value);
            return value;
        }
    }


    public String poll(String itemName) {
        return pollFirst(itemName);
    }

    public String element(String itemName) {
        return getFirst(itemName);
    }

    public String peek(String itemName) {
        return peekFirst(itemName);
    }

    public void push(String itemName,String s) {
        addFirst(itemName, s);
    }

    public String pop(String itemName) {
        return removeFirst(itemName);
    }

    /**
     * 修剪当前 list
     * @param itemName list名称
     * @param start 起始偏移量,  0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @param end  结束偏移量, 0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @return 修剪后的 list 长度
     */
    public boolean trim(String itemName, int start, int end){
        try (Jedis jedis = getJedis()) {
            return eval(jedis, "lrem", itemName, Long.valueOf(start), Long.valueOf(end)).equals("OK");
        }
    }

    /**
     * 返回存储在列表里指定范围内的元素
     * @param itemName list名称
     * @param start 起始偏移量,  0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @param end  结束偏移量, 0 是列表里的第一个元素（表头），1 是第二个元素,-1 表示列表里的最后一个元素， -2 表示倒数第二个
     * @return 修剪后的 list 长度
     */
    public List<String> range(String itemName, int start, int end){
        try (Jedis jedis = getJedis()) {
            return (List<String>)eval(jedis, "lrange", itemName, Long.valueOf(start), Long.valueOf(end));

        }
    }

    public void close() throws IOException {
        redisPool.close();
    }
}
