package org.voovan;

import org.voovan.tools.hashwheeltimer.HashWheelTimer;
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
    private static HashWheelTimer hashWheelTimer;

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
     * 获取一个全局的秒定时器
     *      60个槽位, 每个槽位步长1s
     * @return HashWheelTimer对象
     */
    public synchronized  static HashWheelTimer getHashWheelTimer(){
        if(hashWheelTimer == null) {
            hashWheelTimer = new HashWheelTimer(60, 1000);
            final HashWheelTimer tempTimer = hashWheelTimer;
            hashWheelTimer.rotate();
        }

        return hashWheelTimer;
    }


    /**
     * 获取当前 Voovan 版本号
     * @return Voovan 版本号
     */
    public static String getVersion(){
        return "3.0.0";
    }
}
