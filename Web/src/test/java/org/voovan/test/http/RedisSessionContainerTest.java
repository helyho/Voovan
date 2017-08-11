package org.voovan.http.server;

import org.voovan.tools.RedisMap;
import redis.clients.jedis.JedisPoolConfig;

/**
 * 基于 Redis 的 Seaaion 共享的样例
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisSessionContainerTest extends RedisSessionContainer{

    public RedisSessionContainerTest(){
        super("127.0.0.1", 6379, "123456", 100, 50);
    }
}
