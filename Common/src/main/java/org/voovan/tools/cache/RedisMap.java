package org.voovan.tools.cache;

import redis.clients.jedis.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.AbstractMap;
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
public class RedisMap<K, V> implements CacheMap<K, V>, Closeable {
    public static final String LOCK_SUCCESS = "OK";
    public static final Long   UNLOCK_SUCCESS = 1L;
    public static final String SET_NOT_EXIST = "NX";
    public static final String SET_EXPIRE_TIME = "PX";

    private long expire = 0;


    private JedisPool redisPool;
    private String name = null;
    private int dbIndex = 0;
    private Function<K, V> supplier = null;

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
        this.redisPool = CacheStatic.getDefaultRedisPool();
        this.name = name;
    }

    /**
     * 构造函数
     * @param jedisPool redis 连接池
     * @param name 在 redis 中的 HashMap的名称
     */
    public RedisMap(JedisPool jedisPool, String name){
        this.redisPool = jedisPool;
        this.name = name;
    }

    /**
     * 构造函数
     * @param jedisPool redis 连接池
     */
    public RedisMap(JedisPool jedisPool){
        this.redisPool = jedisPool;
    }

    /**
     * 构造函数
     *      如果 name 为 null,则采用 redis 的顶层键值系统
     */
    public RedisMap(){
        this.redisPool = CacheStatic.getDefaultRedisPool();
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
    public RedisMap<K, V> dbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
        return this;
    }

    private Jedis getJedis(){
        Jedis Jedis = redisPool.getResource();
        Jedis.select(dbIndex);
        return Jedis;
    }

    /**
     * 获取数据创建 Function 对象
     * @return Function 对象
     */
    public Function<K, V> getSupplier(){
        return supplier;
    }

    /**
     * 如果参数为空的默认构造方法
     * @param supplier 创建函数
     */
    public RedisMap<K, V> supplier(Function<K, V> supplier){
        this.supplier = supplier;
        return this;
    }

    /**
     * 获取超时时间
     * @return 超时时间
     */
    public long getExpire() {
        return expire;
    }

    /**
     * 设置超时时间
     * @param expire 超时时间
     */
    public RedisMap<K, V> expire(long expire) {
        this.expire = expire;
        return this;
    }

    @Override
    public int size() {
        try(Jedis jedis = getJedis()) {
            if(name==null){
                return Integer.valueOf(String.valueOf(jedis.dbSize()));
            }else {
                return Integer.valueOf(String.valueOf(jedis.hlen(name)));
            }

        }
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
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

    /**
     * 获取对象
     *
     * @param key 键
     * @param appointedSupplier 在指定的 key 不存在的时候使用指定的获取器构造对象
     * @return 值
     */
    @Override
    public V get(Object key, Function<K, V> appointedSupplier, Long createExpire, boolean refresh){
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray;

        try(Jedis jedis = getJedis()) {
            if(name==null){
                valueByteArray = jedis.get(keyByteArray);
            }else {
                valueByteArray = jedis.hget(name.getBytes(), keyByteArray);
            }
        }

        appointedSupplier = appointedSupplier==null ? supplier : appointedSupplier;
        createExpire = createExpire==null ? expire : createExpire;

        //如果不存在则重读
        if (valueByteArray == null) {
            if(appointedSupplier!=null) {
                synchronized (appointedSupplier) {
                    V value = appointedSupplier.apply((K) key);

                    if(createExpire==0) {
                        put((K) key, value);
                    } else {
                        put((K) key, value, createExpire);
                    }
                    return value;
                }
            }
        }

        //是否刷新超时时间
        if(refresh && name==null){
            if(createExpire!=null) {
                this.setTTL((K) key, createExpire);
            }
        }

        return (V)CacheStatic.unserialize(valueByteArray);
    }

    @Override
    public V put(K key,V value) {
        if(expire!=0){
            return put(key, value, expire);
        }

        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray = CacheStatic.serialize(value);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                if(jedis.set(keyByteArray, valueByteArray).equals(LOCK_SUCCESS)) {
                    //无论是否存在都会返回 OK, 所以默认返回 value
                    return value;
                } else {
                    return null;
                }
            }else {
                if(jedis.hset(name.getBytes(), keyByteArray, valueByteArray)==1){
                    return null;
                } else {
                    return value;
                }
            }
        }
    }

    /**
     * 像 redis 中放置字符串数据
     * @param key key 名称
     * @param value 数据
     * @param expire 超时事件
     * @return true: 成功, false:失败
     */
    public V put(K key, V value, long expire){
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray = CacheStatic.serialize(value);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                if(jedis.setex(keyByteArray, (int) expire, valueByteArray).equals("OK")) {
                    return value;
                } else {
                    return null;
                }
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
        if(expire!=0){
            return putIfAbsent(key, value, expire);
        }

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
                        return get(key);
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
                        return get(key);
                    } else {
                        value = (V) CacheStatic.unserialize(valueBytes);
                    }
                }
            }

            return value;
        }
    }

    public V putIfAbsent(K key, V value, long expire) {
        byte[] keyByteArray = CacheStatic.serialize(key);
        byte[] valueByteArray = CacheStatic.serialize(value);

        try (Jedis jedis = getJedis()) {
            if (name == null) {
                String result = jedis.set(keyByteArray, valueByteArray, SET_NOT_EXIST.getBytes(), SET_EXPIRE_TIME.getBytes(), expire*1000);

                if (LOCK_SUCCESS.equals(result)) {
                    value = null;
                } else {
                    return get(key);
                }
            } else {
                throw new UnsupportedOperationException();
            }

            return value;
        }
    }

    /**
     * 更新某个对象的超时时间
     *      可以为某个没有配置超时时间的键值对配置超时时间
     * @param key 键
     * @param expire 超时时间
     */
    public boolean setTTL(K key, long expire) {
        byte[] keyByteArray = CacheStatic.serialize(key);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.expire(keyByteArray, (int)expire)==1;
            }else {
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * 为 Key 获取 key 的超时时间
     * @param key  key 名称
     * @return 超时时间
     */
    public long getTTL(K key) {
        byte[] keyByteArray = CacheStatic.serialize(key);

        try (Jedis jedis = getJedis()) {
            if(name==null){
                return jedis.ttl(keyByteArray);
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
                    jedis.hdel(name.getBytes(), keyByteArray);
                }
            }

            return (V) CacheStatic.unserialize(valueByteArray);
        }
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        try (Jedis jedis = getJedis()){
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;

                byte[] keyByteArray = CacheStatic.serialize(entry.getKey());
                byte[] valueByteArray = CacheStatic.serialize(entry.getValue());

                if(name==null){
                    jedis.set(keyByteArray, valueByteArray);
                }else {
                    jedis.hset(name.getBytes(), keyByteArray, valueByteArray);
                }
            }
        }
    }

    public void putAll(Map<? extends K, ? extends V> map, long expire) {
        try (Jedis jedis = getJedis()){
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;

                byte[] keyByteArray = CacheStatic.serialize(entry.getKey());
                byte[] valueByteArray = CacheStatic.serialize(entry.getValue());

                if(name==null) {
                    jedis.setex(keyByteArray, (int) expire, valueByteArray);
                } else {
                    throw new UnsupportedOperationException();
                }
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
                return (Set<K>) jedis.keys("*".getBytes()).parallelStream().map(new Function<byte[], Object>() {
                    @Override
                    public Object apply(byte[] bytes) {
                        return  (K)CacheStatic.unserialize(bytes);
                    }
                }).collect(Collectors.toSet());
            }else {
                return (Set<K>)jedis.hkeys(name.getBytes()).parallelStream().map(new Function<byte[], Object>() {
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
                return (Set<K>) jedis.keys(pattern.getBytes()).parallelStream().map(new Function<byte[], Object>() {
                    @Override
                    public Object apply(byte[] bytes) {
                        return  (K)CacheStatic.unserialize(bytes);
                    }
                }).collect(Collectors.toSet());
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
    public Set<Map.Entry<K, V>> entrySet() {
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

    public ScanedObject scan(String cursor, V matchValue, Integer count){
        try (Jedis jedis = getJedis()) {
            byte[] matchValueByteArray = CacheStatic.serialize(matchValue);
            ScanParams scanParams = new ScanParams();
            if(matchValue!=null) {
                scanParams.match(matchValueByteArray);
            }

            if(count!=null) {
                scanParams.count(count);
            }

            if(name==null){
                ScanResult<byte[]> scanResult = jedis.scan(cursor.getBytes(), scanParams);
                ScanedObject scanedObject = new ScanedObject(scanResult.getStringCursor());
                for(byte[] keyBytes : scanResult.getResult()){
                    scanedObject.getResultList().add((V)CacheStatic.unserialize(keyBytes));
                }
                return scanedObject;
            }else {
                ScanResult<Map.Entry<byte[], byte[]>> scanResult = jedis.hscan(name.getBytes(), cursor.getBytes(), scanParams);
                ScanedObject scanedObject = new ScanedObject(scanResult.getStringCursor());

                for(Map.Entry<byte[], byte[]> entryItem : scanResult.getResult()){

                    Map.Entry<K, V> entry = new AbstractMap.SimpleEntry<K, V>((K)CacheStatic.unserialize(entryItem.getKey()), (V)CacheStatic.unserialize(entryItem.getValue()));
                    scanedObject.getResultList().add(entry);
                }
                return scanedObject;
            }



        }
    }
}
