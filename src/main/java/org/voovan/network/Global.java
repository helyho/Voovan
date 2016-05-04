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

    private static ThreadPoolExecutor threadPool;

    /**
     * 返回公用线程池
     * @return 公用线程池
     */
    public static ThreadPoolExecutor getThreadPool(){
       if(threadPool==null || threadPool.isShutdown()){
           threadPool = ThreadPool.getThreadPool();
       }
        return threadPool;
    }
}
