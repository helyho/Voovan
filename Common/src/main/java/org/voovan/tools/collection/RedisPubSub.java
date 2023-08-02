package org.voovan.tools.collection;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

import org.voovan.tools.serialize.TSerialize;

import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.util.Pool;

/**
 * 基于 Redis 的 pubsub 实现
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisPubSub<T> {
    private Pool<Jedis> redisPool;
    private int dbIndex = 0;

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param password    redis 服务密码
     */
    public RedisPubSub(String host, int port, int timeout, int poolsize, String password){
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
    }

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     */
    public RedisPubSub(String host, int port, int timeout, int poolsize){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(poolsize);
        poolConfig.setMaxTotal(poolsize);

        redisPool = new JedisPool(poolConfig, host, port, timeout);
    }

    /**
     * 构造函数
     */
    public RedisPubSub(){
        this.redisPool = CacheStatic.getDefaultRedisPool();
    }

    /**
     * 构造函数
     * @param jedisPool redis 连接池
     */
    public RedisPubSub(Pool<Jedis> jedisPool){
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
    public RedisPubSub<T> dbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
        return this;
    }

    public void close() throws IOException {
        redisPool.close();
    }

    /**
     * 基于基础的序列化方式发布消息
     * @param channel 消息通道名称
     * @param message 消息
     */
    public void pub(String channel, T message) {
        try (Jedis jedis = getJedis()) {
           jedis.publish(channel.getBytes(), TSerialize.serialize(message));
           jedis.pubsubChannels();
        }    
    }

   /**
     * 基于字符串的方式发布消息
     * @param channel 消息通道名称
     * @param message 消息
     */
    public void pubString(String channel, String message) {
        try (Jedis jedis = getJedis()) {
           jedis.publish(channel, message);
           jedis.pubsubChannels();
        }    
    }

    /**
     * 获取所有的消息通道
     * @return 所有的消息通道
     */
    public List<String> allChannels() {
        try (Jedis jedis = getJedis()) {
           return jedis.pubsubChannels();
        }    
    }
    
    /**
     * 订阅消息
     * @param channel 消息通道名称
     * @param consumer 消息消费者
     */
    public void sub(String channel, BiConsumer<String, T> consumer) {
        try (Jedis jedis = getJedis()) {

           jedis.subscribe(new BinaryJedisPubSub() {
            public void onMessage(byte[] channel, byte[] message) {
                consumer.accept(new String(channel), (T)TSerialize.unserialize(message));
            }
           }, channel.getBytes());
        }    
    }

    /**
     * 订阅消息
     * @param channel 消息通道名称
     * @param consumer 消息消费者
     */
    public void subString(String channel, BiConsumer<String, String> consumer) {
        try (Jedis jedis = getJedis()) {

           jedis.subscribe(new JedisPubSub() {
            public void onMessage(String channel, String message) {
                consumer.accept(channel, message);
            }
           }, channel);
        }    
    }

    /**
     * 基于序列化的模式匹配订阅消息
     * @param channelPattern 模式匹配的消息通道名称
     * @param consumer 消息消费者
     */
    public void psub(String channelPattern, BiConsumer<String, T> consumer) {
        try (Jedis jedis = getJedis()) {

           jedis.psubscribe(new BinaryJedisPubSub() {
            public void onPMessage(byte[] pattern, byte[] channel, byte[] message) {
                consumer.accept(new String(channel), (T)TSerialize.unserialize(message));
            }
           }, channelPattern.getBytes());
        }    
    }


    /**
     * 基于字符串的模式匹配订阅消息 
     * @param channelPattern 模式匹配的消息通道名称
     * @param consumer 消息消费者
     */
    public void psubString(String channelPattern, BiConsumer<String, String> consumer) {
        try (Jedis jedis = getJedis()) {

           jedis.psubscribe(new JedisPubSub() {
            public void onPMessage(String pattern, String channel, String message) {
                consumer.accept(channel, message);
            }
           }, channelPattern);
        }    
    }
}
