package org.voovan.tools;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 基于 Redis 的 Map 实现
 *      简单实现,key 和 value 都是 String 类型
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisMap implements Map<String, String>, Closeable {
    private Jedis redis;
    private String name;

    public RedisMap(String host, int port, String name){
        super();
        redis = new Jedis(host, port);
        this.name = name;
    }

    public RedisMap(JedisPool jedisPool, String name){
        super();
        redis = jedisPool.getResource();
        this.name = name;
    }

    public boolean auth(String password){
        String value = redis.auth(password);
        if("OK".equals(value)){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public int size() {
        return Integer.valueOf(String.valueOf(redis.hlen(name)));
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    @Override
    public boolean containsKey(Object key) {
        return redis.hexists(name.toString(), key.toString());
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(Object key) {
        return redis.hget(name, key.toString());
    }

    @Override
    public String put(String key, String value) {
        redis.hset(name, key.toString(), value.toString());
        return value;
    }

    @Override
    public String remove(Object key) {
        String value  = redis.hget(name, key.toString());
        redis.hdel(name, key.toString());
        return value;
    }

    @Override
    public void putAll(Map map) {
        for(Object obj : map.entrySet()) {
            Entry entry = (Entry)obj;
            redis.hset(name, entry.getKey().toString(), entry.getValue().toString());
        }
    }

    @Override
    public void clear() {
        redis.del(name);
    }

    @Override
    public Set keySet() {
        return redis.hkeys(name);
    }

    @Override
    public Collection values() {
        return redis.hvals(name);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void close() throws IOException {
        redis.close();
    }
}
