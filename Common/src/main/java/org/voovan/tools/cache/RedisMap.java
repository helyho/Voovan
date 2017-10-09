package org.voovan.tools.cache;

import org.voovan.tools.cache.CacheStatic;
import org.voovan.tools.json.JSON;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Redis 的 Map 实现
 *      简单实现,key 和 value 都是 String 类型
 *      为了支撑多线程达到线程安全所以默认必须采用JedisPoolConfig来支持
 *      每一个操作都获取一个独立的 Jedis 来操作
 *
 *      如果 name 为 null,则采用 redis 的顶层键值系统, 如果 name 为非 null 则使用 redis 的 map 系统
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisMap implements Map<String, String>, Closeable {
    private JedisPool redisPool;
    private String name = null;
    private int dbIndex = 0;

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param name        在 redis 中的 HashMap的名称
     * @param password    redis 服务密码
     */
    public RedisMap(String host, int port, int timeout, int poolsize, String name, String password){
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
    public RedisMap(String host, int port, int timeout, int poolsize, String name){
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
     *      如果 name 为 null,则采用 redis 的顶层键值系统
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     */
    public RedisMap(String host, int port, int timeout, int poolsize){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(poolsize);
        poolConfig.setMaxTotal(poolsize);

        redisPool = new JedisPool(poolConfig, host, port, timeout);
    }

    /**
     * 构造函数
     * @param name 在 redis 中的 HashMap的名称
     */
    public RedisMap(String name){
        this.redisPool = CacheStatic.getRedisPool();
        this.name = name;
    }

    /**
     * 构造函数
     *      如果 name 为 null,则采用 redis 的顶层键值系统
     */
    public RedisMap(){
        this.redisPool = CacheStatic.getRedisPool();
    }

    /**
     * 获取当前选择的数据集
     * @return 数据集序号
     */
    public int getDbIndex() {
        return dbIndex;
    }

    /**
     * 选择当前数据集
     * @param dbIndex 数据集序号
     */
    public void setDbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
    }

    @Override
    public int size() {
        if(name==null){
            throw new UnsupportedOperationException();
        }

        try(Jedis jedis = getJedis()) {
            return Integer.valueOf(String.valueOf(jedis.hlen(name)));
        }
    }

    @Override
    public boolean isEmpty() {
        if(name==null){
            throw new UnsupportedOperationException();
        }

        return size()==0;
    }

    private Jedis getJedis(){
        Jedis Jedis = getJedis();
        Jedis.select(dbIndex);
        return Jedis;
    }

    @Override
    public boolean containsKey(Object key) {
        try(Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.exists(key.toString());
            }else {
                return jedis.hexists(name.toString(), key.toString());
            }
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(Object key) {
        try(Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.get(key.toString());
            }else {
                return jedis.hget(name, key.toString());
            }
        }
    }

    public <T> T getObj(Object key, Class<T> clazz) {
        return (T) JSON.toObject(get(key), clazz);
    }

    @Override
    public String put(String key, String value) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.set(key.toString(), value.toString());
            }else {
                jedis.hset(name, key.toString(), value.toString());
            }
            return value;

        }
    }

    public boolean put(String key, String value, int expire){
        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.setex(key.toString(), expire, value.toString()).equals("OK");
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    public String putObj(String key, Object value) {
        return put(key, JSON.toJSON(value));
    }

    public boolean putObj(String key, Object value, int expire) {
        return put(key, JSON.toJSON(value), expire);
    }

    @Override
    public String putIfAbsent(String key, String value) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                jedis.setnx(key.toString(), value.toString());
            }else {
                jedis.hsetnx(name, key.toString(), value.toString());
            }

            return value;
        }
    }

    public boolean expire(String key, int expire) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.expire(key.toString(), expire)==1;
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    public boolean persist(String key) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.persist(key.toString())==1;
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public String remove(Object key) {
        try(Jedis jedis = getJedis()) {
            String value = null;
            if(name==null){
                value = jedis.get(key.toString());
                jedis.del(key.toString());
            }else {
                value = jedis.hget(name, key.toString());
                jedis.hdel(name, key.toString());
            }
            return value;
        }
    }

    @Override
    public void putAll(Map map) {
        try (Jedis jedis = getJedis()){
            for (Object obj : map.entrySet()) {
                Entry entry = (Entry) obj;
                jedis.hset(name, entry.getKey().toString(), entry.getValue().toString());
            }
        }
    }

    @Override
    public void clear() {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                jedis.flushDB();
            }else {
                jedis.del(name);
            }
        }
    }

    @Override
    public Set keySet() {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                throw new UnsupportedOperationException();
            }else {
                return jedis.hkeys(name);
            }
        }
    }

    public Set keySet(String pattern) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.keys(pattern);
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public Collection values() {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                throw new UnsupportedOperationException();
            }else {
                return jedis.hvals(name);
            }
        }
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public long incr(String key, long value) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                value = jedis.incrBy(key, value);
            }else {
                value = jedis.hincrBy(name, key, value);
                return value;
            }
        }

        return -1;
    }

    public double incrFloat(String key, double value) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                value = jedis.incrByFloat(key, value);
            }else {
                value = jedis.hincrByFloat(name, key, value);
                return value;
            }
        }

        return -1;
    }

    @Override
    public void close() throws IOException {
        redisPool.close();
    }
}
