package org.voovan.tools.cache;

import redis.clients.jedis.*;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RedisZSet<V> implements Closeable {
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
        this.redisPool = CacheStatic.getDefaultRedisPool();
        this.name = name;
    }

    /**
     * 构造函数
     * @param jedisPool redis 连接池
     * @param name 在 redis 中的 HashMap的名称
     */
    public RedisZSet(JedisPool jedisPool, String name){
        this.redisPool = jedisPool;
        this.name = name;
    }

    /**
     * 构造函数
     * @param jedisPool redis 连接池
     */
    public RedisZSet(JedisPool jedisPool){
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
     * 新增一个元素
     * @param values 新的元素
     * @return 新增元素的数量
     */
    public long addAll(Map<V, Double> values){
        try (Jedis jedis = getJedis()) {
            HashMap<byte[], Double> byteMap = new HashMap<byte[], Double>();
            for(Map.Entry<V, Double> value : values.entrySet()){
                byteMap.put(CacheStatic.serialize(value.getKey()), value.getValue());
            }

            return jedis.zadd(name.getBytes(), byteMap);
        }
    }

    /**
     * 新增一个元素
     * @param score 元素的分
     * @param value 新的元素
     * @return 新增元素的数量
     */
    public long add(double score, V value){
        try (Jedis jedis = getJedis()) {
            byte[] valueByteArray = CacheStatic.serialize(value);
            return jedis.zadd(name.getBytes(), score, valueByteArray);
        }
    }

    /**
     * 对 Score 进行自增
     * @param value 进行自增操作的元素
     * @param score 增加值
     * @return 自增后的 score
     */
    public double increase(V value, double score){
        try (Jedis jedis = getJedis()) {
            byte[] valueByteArray = CacheStatic.serialize(value);
            return jedis.zincrby(name.getBytes(), score, valueByteArray);
        }
    }

    /**
     * 获取前集合的大小
     * @return 集合的大小
     */
    public long size(){
        try (Jedis jedis = getJedis()) {
            return jedis.zcard(name.getBytes());
        }
    }

    /**
     * 某一个特定的 score 范围内的成员数量, 包含 min 和 max 的数据
     * @param min score 的最小值
     * @param max score 的最大值
     * @return 成员的数量
     */
    public long scoreRangeCount(double min, double max){
        try (Jedis jedis = getJedis()) {
            return jedis.zcount(name.getBytes(), min, max);
        }
    }

    /**
     * 某一个成员区间内的成员数量, 包含 min 和 max 的数据
     * @param min value 的最小值
     * @param max value 的最大值
     * @return 成员的数量
     */
    public long valueRangeCount(V min, V max){
        try (Jedis jedis = getJedis()) {
            byte[] minByteValue = CacheStatic.serialize(min);
            byte[] maxByteValue = CacheStatic.serialize(max);
            return jedis.zlexcount(name.getBytes(), minByteValue, maxByteValue);
        }
    }

    /**
     * 某一个特定倒序索引区间内的所有成员, 包含 start 和 end 的数据
     * @param start 索引起始位置
     * @param end value 索引结束位置
     * @return 成员对象的集合
     */
    public Set<V> getRangeByIndex(long start, long end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            Set<byte[]> bytesSet = jedis.zrange(name.getBytes(), start, end);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }


    /**
     * 某一个特定倒序索引区间内的所有成员, 包含 start 和 end 的数据
     * @param start 索引起始位置
     * @param end value 索引结束位置
     * @return 成员对象的集合
     */
    public Set<V> getRevRangeByIndex(long start, long end){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            Set<byte[]> bytesSet = jedis.zrevrange(name.getBytes(), start, end);
            for(byte[] objByteArray : bytesSet){
                result.add((V)CacheStatic.unserialize(objByteArray));
            }
            return result;
        }
    }

    /**
     * 某一个特定值区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @return 成员对象的集合
     */
    public Set<V> getRrangeByValue(V start, V end){
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


    /**
     * 某一个特定值区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<V> getRangeByValue(V start, V end, int offset, int size){
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

    /**
     * 某一个特定值倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @return 成员对象的集合
     */
    public Set<V> getRevRangeByValue(V start, V end){
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

    /**
     * 某一个特定值倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<V> getRevRangeByValue(V start, V end, int offset, int size){
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

    /**
     * 某一个Score区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @return 成员对象的集合
     */
    public Set<V> getRangeByScore(double start, double end){
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

    /**
     * 某一个Score区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最小值
     * @param end value 的最大值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<V> getRangeByScore(double start, double end, int offset, int size){
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

    /**
     * 某一个Score倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @return 成员对象的集合
     */
    public Set<V> getRevRangeByScore(double start, double end){
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

    /**
     * 某一个Score倒序区间内的所有成员, 包含 start 和 end 的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @param offset 偏移量
     * @param size 数量
     * @return 成员对象的集合
     */
    public Set<V> getRevRangeByScore(double start, double end, int offset, int size){
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

    /**
     * 获得当前值的索引位置
     * @param value 值
     * @return 索引诶只
     */
    public long indexOf(V value){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] valueByteArray = CacheStatic.serialize(value);

            return jedis.zrank(name.getBytes(), valueByteArray);
        }
    }

    /**
     * 获得当前值的倒序索引位置
     * @param value 值
     * @return 索引诶只
     */
    public long revIndexOf(V value){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] valueByteArray = CacheStatic.serialize(value);

            return jedis.zrevrank(name.getBytes(), valueByteArray);
        }
    }

    /**
     * 移除某个特定 value
     * @param value 移除的值
     * @return 移除元素的索引
     */
    public long remove(V value){
        try (Jedis jedis = getJedis()) {
            Set<V> result = new HashSet<V>();
            byte[] valueByteArray = CacheStatic.serialize(value);

            return jedis.zrem(name.getBytes(), valueByteArray);
        }
    }

    /**
     * 移除某个特定 value 区间的数据
     * @param start value 的最大值
     * @param end value 的最小值
     * @return 移除元素的数量
     */
    public long removeRangeByValue(V start, V end){
        try (Jedis jedis = getJedis()) {
            byte[] startByteArray = CacheStatic.serialize(start);
            byte[] endByteArray = CacheStatic.serialize(end);

            return jedis.zremrangeByLex(name.getBytes(), startByteArray, endByteArray);
        }
    }

    /**
     * 移除某个特定索引区间的数据
     * @param start 索引起始位置
     * @param end value 索引结束位置
     * @return 移除元素的数量
     */
    public long removeRangeByIndex(int start, int end){
        try (Jedis jedis = getJedis()) {
            return jedis.zremrangeByRank(name.getBytes(), start, end);
        }
    }

    /**
     * 移除某个特定Score区间的数据
     * @param min score 的最小值
     * @param max score 的最大值
     * @return 移除元素的数量
     */
    public long removeRangeByScore(double min, double max){
        try (Jedis jedis = getJedis()) {
            return jedis.zremrangeByScore(name.getBytes(), min, max);
        }
    }

    /**
     * 获取某个特定值的 Score
     * @param value 值
     * @return 对应的 Score
     */
    public double getScore(V value){
        try (Jedis jedis = getJedis()) {
            byte[] valueByteArray = CacheStatic.serialize(value);
            return jedis.zscore(name.getBytes(),valueByteArray);
        }
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
            ScanResult<Tuple> scanResult = jedis.zscan(name.getBytes(), cursor.getBytes(), scanParams);

            ScanedObject scanedObject = new ScanedObject(scanResult.getStringCursor());
            for(Tuple tuple : scanResult.getResult()){
                scanedObject.getResultList().add((V)CacheStatic.unserialize(tuple.getBinaryElement()));
            }
            return scanedObject;
        }
    }

    @Override
    public void close() throws IOException {
        redisPool.close();
    }
}
