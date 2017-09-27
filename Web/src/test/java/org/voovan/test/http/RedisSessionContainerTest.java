package org.voovan.test.http;

import org.voovan.tools.cache.RedisMap;

/**
 * 基于 Redis 的 Seaaion 共享的样例
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class RedisSessionContainerTest extends RedisMap {

    public RedisSessionContainerTest(){
        super("10.0.0.101", 6379, 2000, 100);
    }
}
