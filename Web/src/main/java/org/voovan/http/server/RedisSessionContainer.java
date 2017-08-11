package org.voovan.http.server;

import org.voovan.tools.RedisMap;
import org.voovan.tools.TPerformance;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisSessionContainer extends RedisMap{

    /**
     * 构造一个基于 Redis 的 Session 容器
     * @param host      redis 服务地址
     * @param port      redis 服务端口
     * @param password  redis 服务密码
     * @param poolTotal redis 池的最大数量
     * @param poolIdle  redis 池的空闲连接数
     */
    public RedisSessionContainer(String host, int port, String password, int poolTotal, int poolIdle){
        super(getJedisPoolConfig(poolTotal, poolIdle), host, port, "VOOVAN_WEB_SESSIONS", password);
    }

    /**
     * 构造一个基于 Redis 的 Session 容器
     * @param host      redis 服务地址
     * @param port      redis 服务端口
     * @param password  redis 服务密码
     */
    public RedisSessionContainer(String host, int port, String password){
        super(getJedisPoolConfig(-1, -1), host, port, "VOOVAN_WEB_SESSIONS", password);
    }

    /**
     * 获取一个JedisPoolConfig对象
     * @param poolTotal redis 池的最大数量
     * @param poolIdle redis 池的空闲连接数
     * @return JedisPoolConfig对象
     */
    public static JedisPoolConfig getJedisPoolConfig(int poolTotal, int poolIdle){

        if(poolTotal == -1 || poolIdle == -1) {
            int processorCount = TPerformance.getProcessorCount();
            poolTotal = processorCount * 2;
            poolIdle = poolTotal;
        }


        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(poolTotal);
        poolConfig.setMaxIdle(poolIdle);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        return poolConfig;
    }
}
