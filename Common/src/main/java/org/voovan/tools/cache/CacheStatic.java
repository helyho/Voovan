package org.voovan.tools.cache;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.voovan.tools.TPerformance;
import org.voovan.tools.TProperties;
import org.voovan.tools.TSerialize;
import org.voovan.tools.log.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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

    /**
     * 获取一个 MemcachedClientBuilder 也就是 Memcached的连接池
     * @return MemcachedClientBuilder 对象
     */
    public static MemcachedClientBuilder getMemcachedPool(){
        if(memcachedClientBuilder == null) {
            try {
                String host = TProperties.getString("memcached", "Host");
                int port = TProperties.getInt("memcached", "Port");
                int timeout = TProperties.getInt("memcached", "Timeout");
                int poolSize = TProperties.getInt("memcached", "PoolSize");

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
                String host = TProperties.getString("redis", "Host");
                int port = TProperties.getInt("redis", "Port");
                int timeout = TProperties.getInt("redis", "Timeout");
                String password = TProperties.getString("redis", "Password");
                int poolSize = TProperties.getInt("redis", "PoolSize");

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

    /**
     * 获取 Redis 的连接
     * @param dbIndex 数据集序号
     * @return Jedis 对象
     */
    public static Jedis getRedisClient(int dbIndex){

        if(redisPool==null){
            getRedisPool();
        }

        if(redisPool != null) {
            return redisPool.getResource();
        }else{
            return null;
        }
    }

    /**
     * 序列化
     * @param obj 待序列化的对象
     * @return 字节码
     */
    public static byte[] serialize(Object obj){
        if(obj == null){
            return null;
        }

        if( obj instanceof Integer){
            return ((Integer) obj).toString().getBytes();
        }
        else if( obj instanceof Long){
            return ((Long) obj).toString().getBytes();
        }
        else if( obj instanceof Short){
            return ((Short) obj).toString().getBytes();
        }
        else if( obj instanceof Float){
            return ((Float) obj).toString().getBytes();
        }
        else if( obj instanceof Double){
            return ((Double) obj).toString().getBytes();
        }
        else if( obj instanceof Character){
            return ((Character)obj).toString().getBytes();
        }
        else if( obj instanceof String){
            return ((String)obj).toString().getBytes();
        }
        else {
            return TSerialize.serialize(obj);
        }
    }

    /**
     * 反序列化
     * @param byteArray 字节码
     * @return 反序列化的对象
     */
    public static Object unserialize(byte[] byteArray){
        if(byteArray==null){
            return null;
        }

        if(byteArray[0]==-84 && byteArray[1]==-19 && byteArray[2]==0 && byteArray[3]==5){
            return TSerialize.unserialize(byteArray);
        } else {
            return new String(byteArray);
        }
    }

}
