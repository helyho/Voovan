package org.voovan;

import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.threadpool.ThreadPool;

import java.io.File;
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

    public static String NAME = "Voovan";

    private static ThreadPoolExecutor threadPool;
    private static HashWheelTimer hashWheelTimer;

    private static File frameworkConfigFile = getFrameworkConfigFile();
    public static volatile Boolean noHeapManualRelease = null;

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
    public synchronized static HashWheelTimer getHashWheelTimer(){
        if(hashWheelTimer == null) {
            hashWheelTimer = new HashWheelTimer(60, 1000);
            hashWheelTimer.rotate();
        }

        return hashWheelTimer;
    }

    /**
     * 读取非堆内存配置文件信息
     * @return 日志配置文件对象
     */
    protected static File getFrameworkConfigFile(){
        File tmpFile =  tmpFile = new File("./classes/framework.properties");

        //如果从 classes 目录中找不到,则从 classpath 中寻找
        if(tmpFile==null || !tmpFile.exists()){
            tmpFile = TFile.getResourceFile("framework.properties");
        }

        if(tmpFile!=null){
            return tmpFile;
        }else{
            System.out.println("Waring: Can't found log noheap file!");
            System.out.println("Waring: System will be use default noheap config: {Noheap:false, release:false}");
            return null;
        }
    }

    /**
     * 非对内存是否采用手工释放
     * @return true: 收工释放, false: JVM自动释放
     */
    public static boolean isNoHeapManualRelease() {
        if(noHeapManualRelease == null) {
            boolean value = false;
            if (frameworkConfigFile != null) {
                value = TProperties.getBoolean(frameworkConfigFile, "NoHeapManualRelease");
            }
            noHeapManualRelease = TObject.nullDefault(value, false);
            System.out.println("NoHeap Manual Release: " + noHeapManualRelease);
        }

        return noHeapManualRelease;
    }

    /**
     * 获取当前 Voovan 版本号
     * @return Voovan 版本号
     */
    public static String getVersion(){
        return "3.0.0";
    }
}
