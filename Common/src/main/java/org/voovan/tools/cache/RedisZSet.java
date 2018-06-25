package org.voovan.tools.cache;

import redis.clients.jedis.*;
import redis.clients.jedis.params.sortedset.ZAddParams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RedisZSet<V>  {
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
    public RedisZSet(String host, int port, int timeout, int poolsize, String name, String password){
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
    public RedisZSet(String host, int port, int timeout, int poolsize, String name){
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
    public RedisZSet(String name){
        this.redisPool = CacheStatic.getRedisPool();
        this.name = name;
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

    public long addIfAbsent(double score, V value){
        try (Jedis jedis = getJedis()) {
            byte[] valueByteArray = CacheStatic.serialize(value);
            return jedis.zadd(name.getBytes(), score, valueByteArray, ZAddParams.zAddParams().nx());
        }
    }

    public long update(double score, V value){
        try (Jedis jedis = getJedis()) {
            byte[] valueByteArray = CacheStatic.serialize(value);
            return jedis.zadd(name.getBytes(), score, valueByteArray, ZAddParams.zAddParams().xx().ch());
        }
    }

    public double increase(double score, V value){
        try (Jedis jedis = getJedis()) {
            byte[] valueByteArray = CacheStatic.serialize(value);
            return jedis.zincrby(name.getBytes(), score, valueByteArray);
        }
    }

    public long size(double score, V value){
        try (Jedis jedis = getJedis()) {
            return jedis.zcard(name.getBytes());
        }
    }

    public long rangeCount(double score, double min, double max){
        try (Jedis jedis = getJedis()) {
            return jedis.zcount(name.getBytes(), min, max);
        }
    }

    public long scoreRangeCount(double score, double min, double max){
        try (Jedis jedis = getJedis()) {
            return jedis.zcount(name.getBytes(), min, max);
        }
    }

    public long valueRangeCount(byte[] min, byte[] max){
        try (Jedis jedis = getJedis()) {
            return jedis.zlexcount(name.getBytes(), min, max);
        }
    }

    public Set<V> rangeByIndex(long start, long end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            Set<byte[]> bytesSet = jedis.zrange(name.getBytes(), start, end);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> revRangeByIndex(long start, long end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            Set<byte[]> bytesSet = jedis.zrevrange(name.getBytes(), start, end);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> rangeByValue(V start, V end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrangeByLex(name.getBytes(), startByteArray, endByteArray);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> rangeByValue(V start, V end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrangeByLex(name.getBytes(), startByteArray, endByteArray, offset, size);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> revRangeByValue(V start, V end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrevrangeByLex(name.getBytes(), startByteArray, endByteArray);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> revRangeByValue(V start, V end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrevrangeByLex(name.getBytes(), startByteArray, endByteArray, offset, size);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> rangeByScore(double start, double end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrangeByScore(name.getBytes(), startByteArray, endByteArray);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> rangeByScore(double start, double end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrangeByScore(name.getBytes(), startByteArray, endByteArray, offset, size);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> revRangeByScore(double start, double end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrevrangeByScore(name.getBytes(), startByteArray, endByteArray);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public Set<V> revRangeByScore(double start, double end, int offset, int size){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            Set<byte[]> bytesSet = jedis.zrevrangeByScore(name.getBytes(), startByteArray, endByteArray, offset, size);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    public long indexOf(V value){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] valueByteArray = CacheStatic.serialize(value);

            return jedis.zrank(name.getBytes(), valueByteArray);
        }
    }

    public long revIndexOf(V value){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] valueByteArray = CacheStatic.serialize(value);

            return jedis.zrevrank(name.getBytes(), valueByteArray);
        }
    }

    public long remove(V value){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] valueByteArray = CacheStatic.serialize(value);

            return jedis.zrem(name.getBytes(), valueByteArray);
        }
    }

    public long removeRangeByValue(V start, V end){
        try (Jedis jedis = getJedis()) {
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            return jedis.zremrangeByLex(name.getBytes(), startByteArray, endByteArray);
        }
    }

    public long removeRangeByIndex(int start, int end){
        try (Jedis jedis = getJedis()) {
            return jedis.zremrangeByRank(name.getBytes(), start, end);
        }
    }

    public long removeRangeByIndex(double start, double end){
        try (Jedis jedis = getJedis()) {
            return jedis.zremrangeByScore(name.getBytes(), start, end);
        }
    }

    public double getScore(V value){
        try (Jedis jedis = getJedis()) {
            byte[] valueByteArray = CacheStatic.serialize(value);
            return jedis.zscore(name.getBytes(),valueByteArray);
        }
    }

    public ScanedObject newScan(String cursor, V matchValue, int count){
        try (Jedis jedis = getJedis()) {
            byte[] matchValueByteArray = CacheStatic.serialize(matchValue);
            ScanParams scanParams = new ScanParams();
            scanParams.match(matchValueByteArray);
            scanParams.count(count);
            ScanResult<Tuple> scanResult = jedis.zscan(name.getBytes(), cursor.getBytes(), scanParams);

            ScanedObject scanedObject = new ScanedObject(scanResult.getStringCursor());
            for(Tuple tuple : scanResult.getResult()){
                scanedObject.getResultList().add((V)CacheStatic.unserialize(tuple.getBinaryElement()));
            }
            return scanedObject;
        }
    }


    public class  ScanedObject<V>{
        private String cursor;
        private List<V> resultList;

        public ScanedObject(String cursor) {
            this.cursor = cursor;
            this.resultList = new ArrayList<V>();
        }

        public String getCursor() {
            return cursor;
        }

        public void setCursor(String cursor) {
            this.cursor = cursor;
        }

        public List<V> getResultList() {
            return resultList;
        }

        public void setResultList(List<V> resultList) {
            this.resultList = resultList;
        }
    }
}
