package org.voovan.network;

import org.voovan.tools.threadpool.ThreadPool;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by helyho on 16/3/1.
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
