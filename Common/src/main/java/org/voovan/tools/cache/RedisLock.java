package org.voovan.tools.cache;

import org.voovan.tools.TEnv;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Collections;

/**
 * 基于 Redis 的分布式锁
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class RedisLock {
    private JedisPool redisPool;
    private String lockName = null;
    private int dbIndex = 0;
    private String lockValue = null;

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param lockName        锁的键名
     * @param password    redis 服务密码
     */
    public RedisLock(String host, int port, int timeout, int poolsize, String lockName, String password){
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
        this.lockName = lockName;
    }

    /**
     * 构造函数
     * @param host        redis 服务地址
     * @param port        redis 服务端口
     * @param timeout     redis 连接超时时间
     * @param poolsize    redis 连接池的大小
     * @param lockName        锁的键名称
     */
    public RedisLock(String host, int port, int timeout, int poolsize, String lockName){
        super();

        //如果没有指定JedisPool的配置文件,则使用默认的
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxIdle(poolsize);
        poolConfig.setMaxTotal(poolsize);

        redisPool = new JedisPool(poolConfig, host, port, timeout);
        this.lockName = lockName;
    }

    /**
     * 构造函数
     * @param lockName 锁的键名
     */
    public RedisLock(String lockName){
        this.redisPool = CacheStatic.getRedisPool();
        this.lockName = lockName;
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

    private Jedis getJedis(){
        Jedis Jedis = redisPool.getResource();
        Jedis.select(dbIndex);
        return Jedis;
    }

    /**
     * 尝试加锁
     *      快速失败的方式
     * @param lockValue 锁的值
     * @param lockExpireTime 锁的超时时间
     * @return true: 成功, false: 失败
     */
    public boolean tryLock(String lockValue, int lockExpireTime){

        if(lockValue==null){
            lockValue = String.valueOf(System.currentTimeMillis());
        }

        try (Jedis jedis = getJedis()) {
            String result = jedis.set(this.lockName, lockValue, RedisMap.SET_NOT_EXIST, RedisMap.SET_EXPIRE_TIME, lockExpireTime);
            this.lockValue = lockValue;

            if ( RedisMap.LOCK_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        }
    }

    /**
     * 尝试加锁
     *      快速失败的方式
     * @param lockExpireTime 超时时间
     * @return true: 成功, false: 失败
     */
    public boolean tryLock(int lockExpireTime){
        return tryLock(null, lockExpireTime);
    }

    /**
     * 尝试加锁
     *      超时时间 timewait 是尝试加锁的最长时间
     * @param lockValue 锁的值
     * @param lockExpireTime 锁的键值对的超时时间
     * @param timewait 获取锁的等待时间, 单位: 毫秒
     * @return true: 成功, false: 失败
     */
    public boolean lock(String lockValue, int lockExpireTime, int timewait){

        if(lockValue==null){
            lockValue = String.valueOf(System.currentTimeMillis());
            this.lockValue = lockValue;
        }
        while(true) {
            try (Jedis jedis = getJedis()) {
                String result = jedis.set(this.lockName, lockValue,  RedisMap.SET_NOT_EXIST,  RedisMap.SET_EXPIRE_TIME, lockExpireTime);

                if (RedisMap.LOCK_SUCCESS.equals(result)) {
                    return true;
                }
                timewait--;

                if(timewait<=0){
                    break;
                }

                TEnv.sleep(1);
                continue;
            }
        }

        return false;
    }

    /**
     * 尝试加锁
     *      超时时间 timewait 是尝试加锁的最长时间
     * @param lockExpireTime 锁的键值对的超时时间
     * @param timewait 获取锁的等待时间, 单位: 毫秒
     * @return true: 成功, false: 失败
     */
    public boolean lock(int lockExpireTime, int timewait){
        return lock(null, lockExpireTime, timewait);
    }

    /**
     * 释放锁
     * @param lockValue 锁
     * @return true: 成功, false: 失败
     */
    public boolean unLock(String lockValue) {

        try (Jedis jedis = getJedis()) {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(this.lockName), Collections.singletonList(lockValue));

            if ( RedisMap.UNLOCK_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        }

    }

    /**
     * 释放锁
     * @return true: 成功, false: 失败
     */
    public boolean unLock() {

        try (Jedis jedis = getJedis()) {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Object result = jedis.eval(script, Collections.singletonList(this.lockName), Collections.singletonList(this.lockValue));

            if ( RedisMap.UNLOCK_SUCCESS.equals(result)) {
                return true;
            }
            return false;
        }

    }
}
