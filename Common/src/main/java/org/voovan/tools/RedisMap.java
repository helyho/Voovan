package org.voovan.tools;

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
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisMap implements Map<String, String>, Closeable {
    private JedisPool redisPool;
    private String name;

    /**
     * 构造函数
     * @param poolConfig  Jedis 连接池的配置对象
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param name        在 redis 中的 HashMap的名称
     * @param password    redis 服务密码
     */
    public RedisMap(JedisPoolConfig poolConfig, String host, int port, String name, String password){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        if(poolConfig == null) {
            poolConfig = new JedisPoolConfig();
        }

        //如果没有指定密码,则默认不需要密码
        if(password==null) {
            redisPool = new JedisPool(poolConfig, host, port, 2000);
        }else {
            redisPool = new JedisPool(poolConfig, host, port, 2000, password);
        }
        this.name = name;
    }

    @Override
    public int size() {
        try(Jedis jedis = redisPool.getResource()) {
            return Integer.valueOf(String.valueOf(jedis.hlen(name)));
        }
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public boolean containsKey(Object key) {
        try(Jedis jedis = redisPool.getResource()) {
            return jedis.hexists(name.toString(), key.toString());
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(Object key) {
        try(Jedis jedis = redisPool.getResource()) {
            return jedis.hget(name, key.toString());
        }
    }

    @Override
    public String put(String key, String value) {
        try (Jedis jedis = redisPool.getResource()) {
            jedis.hset(name, key.toString(), value.toString());
            return value;
        }
    }

    @Override
    public String remove(Object key) {
        try(Jedis jedis = redisPool.getResource()) {
            String value = jedis.hget(name, key.toString());
            jedis.hdel(name, key.toString());
            return value;
        }
    }

    @Override
    public void putAll(Map map) {
        try (Jedis jedis = redisPool.getResource()){
            for (Object obj : map.entrySet()) {
                Entry entry = (Entry) obj;
                jedis.hset(name, entry.getKey().toString(), entry.getValue().toString());
            }
        }
    }

    @Override
    public void clear() {
        try (Jedis jedis = redisPool.getResource()) {
            jedis.del(name);
        }
    }

    @Override
    public Set keySet() {
        try (Jedis jedis = redisPool.getResource()) {
            return jedis.hkeys(name);
        }
    }

    @Override
    public Collection values() {
        try (Jedis jedis = redisPool.getResource()) {
            return jedis.hvals(name);
        }
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void close() throws IOException {
        redisPool.close();
    }
}
