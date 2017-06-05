package org.voovan;

import org.voovan.tools.threadpool.ThreadPool;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
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
    public synchronized static ThreadPoolExecutor getThreadPool(){
       if(threadPool==null || threadPool.isShutdown()){
           threadPool = ThreadPool.getNewThreadPool();
       }

       return threadPool;
    }

    /**
     * 获取当前 Voovan 版本号
     * @return Voovan 版本号
     */
    public static String getVersion(){
        return "1.5.3";
    }
}
