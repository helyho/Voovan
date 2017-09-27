package org.voovan.tools.cache;

import org.voovan.tools.TPerformance;
import org.voovan.tools.TProperties;
import org.voovan.tools.log.Logger;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.io.IOException;

/**
 * 缓存静态类
 *  用于准备 memcached 和 redis 的连接池
 *
 * @author: helyho
 * Project: Framework
 * Create: 2017/9/26 18:30
 */
public class CacheStatic {

    private static MemcachedClientBuilder memcachedClientBuilder = null;
    private static JedisPool redisPool = null;
    private static File memecacheConfigFile = new File("./classes/memcached.properties");
    private static File redisConfigFile = new File("./classes/redis.properties");


    /**
     * 获取一个 MemcachedClientBuilder 也就是 Memcached的连接池
     * @return MemcachedClientBuilder 对象
     */
    public static MemcachedClientBuilder getMemcachedPool(){
        if(memcachedClientBuilder == null) {
            try {
                String host = TProperties.getString(memecacheConfigFile, "Host");
                int port = TProperties.getInt(memecacheConfigFile, "Port");
                int timeout = TProperties.getInt(memecacheConfigFile, "Timeout");
                int poolSize = TProperties.getInt(memecacheConfigFile, "PoolSize");

                if(host==null){
                    return null;
                }

                if (poolSize == 0) {
                    poolSize = defaultPoolSize();
                }

                memcachedClientBuilder = new XMemcachedClientBuilder(
                        AddrUtil.getAddresses(host + ":" + port));
                memcachedClientBuilder.setFailureMode(true);
                memcachedClientBuilder.setCommandFactory(new BinaryCommandFactory());
                memcachedClientBuilder.setConnectionPoolSize(poolSize);
                memcachedClientBuilder.setConnectTimeout(timeout);
            }catch (Exception e){
                Logger.error("Read ./classes/Memcached.properties error");
            }
        }

        return memcachedClientBuilder;
    }


    /**
     * 获取一个 RedisPool 连接池
     * @return JedisPool 对象
     */
    public static JedisPool getRedisPool(){
        if(redisPool == null) {
            try {
                String host = TProperties.getString(redisConfigFile, "Host");
                int port = TProperties.getInt(redisConfigFile, "Port");
                int timeout = TProperties.getInt(redisConfigFile, "Timeout");
                String password = TProperties.getString(redisConfigFile, "Password");
                int poolSize = TProperties.getInt(redisConfigFile, "PoolSize");

                if(host==null){
                    return null;
                }

                if (poolSize == 0) {
                    poolSize = defaultPoolSize();
                }

                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(poolSize);
                poolConfig.setMaxIdle(poolSize);
                poolConfig.setTestOnBorrow(true);
                poolConfig.setTestOnReturn(true);


                if (password == null) {
                    redisPool = new JedisPool(poolConfig, host, port, timeout);
                } else {
                    redisPool = new JedisPool(poolConfig, host, port, timeout, password);
                }
            }catch (Exception e){
                Logger.error("Read ./classes/Memcached.properties error");
            }
        }

        return redisPool;
    }

    /**
     * 获取系统自动计算的连接池的大小
     * @return 连接池的大小
     */
    public static int defaultPoolSize(){
        return TPerformance.getProcessorCount() * 10;
    }

    /**
     * 获取 memcached 连接
     * @return MemcachedClient 对象
     * @throws IOException io 异常
     */
    public static MemcachedClient getMemcachedClient() throws IOException {

        if(memcachedClientBuilder==null){
            getMemcachedPool();
        }

        if(memcachedClientBuilder != null) {
            return memcachedClientBuilder.build();
        }else{
            return null;
        }
    }

    /**
     * 获取 Redis 的连接
     * @return Jedis 对象
     */
    public static Jedis getRedisClient(){

        if(redisPool==null){
            getRedisPool();
        }

        if(redisPool != null) {
            return redisPool.getResource();
        }else{
            return null;
        }
    }

}
