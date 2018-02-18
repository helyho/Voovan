package org.voovan.tools.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class RedisMap<K, V> implements Map<K, V>, Closeable {
    private JedisPool redisPool;
    private String name = null;
    private int dbIndex = 0;
    private Function<K, V> buildFunction = null;

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

    public void build(Function<K, V> buildFunction){
        this.buildFunction = buildFunction;
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
        Jedis Jedis = redisPool.getResource();
        Jedis.select(dbIndex);
        return Jedis;
    }

    @Override
    public boolean containsKey(Object key) {
        byte[] keyByteArray = CacheStatic.serialize(key);

        try(Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.exists(keyByteArray);
            }else {
                return jedis.hexists(name.getBytes(), keyByteArray);
            }
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray;

        try(Jedis jedis = getJedis()) {
            if(name==null){
                valueByteArray = jedis.get(keyByteArray);
            }else {
                valueByteArray = jedis.hget(name.getBytes(), keyByteArray);
            }
        }

        //如果不存在则重读
        if (valueByteArray == null) {
            if(buildFunction!=null) {
                synchronized (buildFunction) {
                    V value = buildFunction.apply((K) key);
                    put((K) key, value);
                    return value;
                }
            }
        }


        return (V)CacheStatic.unserialize(valueByteArray);
    }

    @Override
    public V put(K key,V value) {
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray = CacheStatic.serialize(value);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                jedis.set(keyByteArray, valueByteArray);
            }else {
                jedis.hset(name.getBytes(), keyByteArray, valueByteArray);
            }
            return value;

        }
    }

    /**
     * 像 redis 中放置字符串数据
     * @param key key 名称
     * @param value 数据
     * @param expire 超时事件
     * @return true: 成功, false:失败
     */
    public boolean put(K key, V value, int expire){
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray = CacheStatic.serialize(value);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.setex(keyByteArray, expire, valueByteArray).equals("OK");
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * 在 Redis 环境下返回的数据不保证是 Redis 中的数据
     * @param key   更新的 key
     * @param value 更新的 value,
     * @return 返回的数据对象, value: null更新成功, value: 有数据返回的情况下仅仅表示更新失败, 返回的数据无法准确视为 Redis 中保存的数据
     */
    @Override
    public V putIfAbsent(K key, V value) {
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray = CacheStatic.serialize(value);

        try (Jedis jedis = getJedis()) {
            long result = 0;
            if(name==null){
                result = jedis.setnx(keyByteArray, valueByteArray);

                if(result==1) {
                    value = null;
                } else {
                    byte[] valueBytes = jedis.get(keyByteArray);
                    if(valueBytes == null){
                        return value;
                    } else {
                        value = (V) CacheStatic.unserialize(valueBytes);
                    }
                }
            }else {
                result = jedis.hsetnx(name.getBytes(), keyByteArray, valueByteArray);

                if(result==1) {
                    value = null;
                } else {
                    byte[] valueBytes = jedis.hget(name.getBytes(), keyByteArray);
                    if(valueBytes == null){
                        return value;
                    } else {
                        value = (V) CacheStatic.unserialize(valueBytes);
                    }
                }
            }

            return value;
        }
    }

    /**
     * 为 Key 设置超时事件
     * @param key  key 名称
     * @param expire 超时事件
     * @return true: 成功, false:失败
     */
    public boolean expire(K key, int expire) {
        byte[] keyByteArray = CacheStatic.serialize(key);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.expire(keyByteArray, expire)==1;
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * 持久化某个
     * @param key key 名称
     * @return true: 成功, false:失败
     */
    public boolean persist(K key) {
        byte[] keyByteArray = CacheStatic.serialize(key);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.persist(keyByteArray)==1;
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public V remove(Object key) {
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray;

        try(Jedis jedis = getJedis()) {
            byte[] value = null;
            if(name==null){
                valueByteArray = jedis.get(keyByteArray);
                if(valueByteArray!=null) {
                    jedis.del(keyByteArray);
                }
            }else {
                valueByteArray = jedis.hget(name.getBytes(), keyByteArray);
                if(valueByteArray!=null) {
                    jedis.hdel(keyByteArray);
                }
            }

            return (V) CacheStatic.unserialize(valueByteArray);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        try (Jedis jedis = getJedis()){
            for (Object obj : map.entrySet()) {
                Entry entry = (Entry) obj;

                byte[] keyByteArray = CacheStatic.serialize(entry.getKey());
                byte[] valueByteArray = CacheStatic.serialize(entry.getValue());

                jedis.hset(name.getBytes(), keyByteArray, valueByteArray);
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
    public Set<K> keySet() {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                throw new UnsupportedOperationException();
            }else {
                return (Set<K>)jedis.hkeys(name.getBytes()).stream().map(new Function<byte[], Object>() {
                    @Override
                    public Object apply(byte[] bytes) {
                        return  (K)CacheStatic.unserialize(bytes);
                    }
                }).collect(Collectors.toSet());
            }
        }
    }

    /**
     * 获取键集合
     * @param pattern 匹配表达式
     * @return 键集合
     */
    public Set<K> keySet(String pattern) {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                return (Set<K>)jedis.hkeys(pattern.getBytes()).stream().map(new Function<byte[], K>() {
                    @Override
                    public K apply(byte[] bytes) {
                        return  (K)CacheStatic.unserialize(bytes);
                    }
                }).collect(Collectors.toSet());
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @Override
    public Collection<V> values() {
        try (Jedis jedis = getJedis()) {
            if(name==null){
                throw new UnsupportedOperationException();
            }else {
                return jedis.hvals(name.getBytes()).stream().map(new Function<byte[], V>() {
                    @Override
                    public V apply(byte[] bytes) {
                        return  (V)CacheStatic.unserialize(bytes);
                    }
                }).collect(Collectors.toList());
            }
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * 原子增加操作
     * @param key  key 名称
     * @param value 值
     * @return 自增后的结果
     */
    public long incr(K key, long value) {
        byte[] keyByteArray = CacheStatic.serialize(key);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                value = jedis.incrBy(keyByteArray, value);
            }else {
                value = jedis.hincrBy(name.getBytes(), keyByteArray, value);
                return value;
            }
        }

        return -1;
    }

    /**
     * 原子增加操作
     * @param key  key 名称
     * @param value 值
     * @return 自增后的结果
     */
    public double incrFloat(K key, double value) {
        byte[] keyByteArray = CacheStatic.serialize(key);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                value = jedis.incrByFloat(keyByteArray, value);
            }else {
                value = jedis.hincrByFloat(name.getBytes(), keyByteArray, value);
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
