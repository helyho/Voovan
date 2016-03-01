package org.voovan.network;

import org.voovan.tools.threadpool.ThreadPool;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局对象
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Global {

    private static ThreadPoolExecutor threadPool = ThreadPool.getThreadPool();

    /**
     * 返回公用线程池
     * @return
     */
    public static ThreadPoolExecutor getThreadPool(){
        return threadPool;
    }
}
