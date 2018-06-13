package org.voovan;

import org.voovan.tools.TObject;
import org.voovan.tools.TProperties;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.task.TaskManager;
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

    public static String NAME = "Voovan";

    private static HashWheelTimer hashWheelTimer;
    private static TaskManager taskManager;

    public static volatile Boolean NO_HEAP_MANUAL_RELEASE = getNoHeapManualRelease();

    /**
     * 非对内存是否采用手工释放
     * @return true: 收工释放, false: JVM自动释放
     */
    private  static boolean getNoHeapManualRelease() {
        if(NO_HEAP_MANUAL_RELEASE == null) {
            boolean value = false;
            value = TProperties.getBoolean("framework", "NoHeapManualRelease");
            NO_HEAP_MANUAL_RELEASE = TObject.nullDefault(value, false);
            System.out.println("[SYSTEM] NoHeap Manual Release: " + NO_HEAP_MANUAL_RELEASE);
        }

        return NO_HEAP_MANUAL_RELEASE;
    }

    private enum ThreadPoolEnum {
        THREAD_POOL;

        private ThreadPoolExecutor threadPoolExecutor;

        ThreadPoolEnum(){
            threadPoolExecutor = ThreadPool.getNewThreadPool();
        }

        public ThreadPoolExecutor getValue(){
            return threadPoolExecutor;
        }
    }

    /**
     * 返回公用线程池
     * @return 公用线程池
     */
    public static ThreadPoolExecutor getThreadPool(){
        return ThreadPoolEnum.THREAD_POOL.getValue();
    }


    private enum HashTimeWheelEnum {
        HASHWHEEL;

        private HashWheelTimer hashWheelTimer;
        HashTimeWheelEnum (){
            hashWheelTimer = new HashWheelTimer(60, 1000);
            hashWheelTimer.rotate();
        }

        public HashWheelTimer getValue(){
            return hashWheelTimer;
        }
    }

    /**
     * 获取一个全局的秒定时器
     *      60个槽位, 每个槽位步长1s
     * @return HashWheelTimer对象
     */
    public static HashWheelTimer getHashWheelTimer(){
        return HashTimeWheelEnum.HASHWHEEL.getValue();
    }


    private enum TaskManagerEnum {
        TASK_MANAGER;

        private TaskManager taskManager;
        TaskManagerEnum(){
            taskManager = new TaskManager();
            taskManager.scanTask();
        }
        public TaskManager getValue(){
            return taskManager;
        }
    }

    /**
     * 获取一个全局的秒定时器
     *      60个槽位, 每个槽位步长1ms
     * @return HashWheelTimer对象
     */
    public static TaskManager getTaskManager(){

        return TaskManagerEnum.TASK_MANAGER.getValue();
    }

    /**
     * 获取当前 Voovan 版本号
     * @return Voovan 版本号
     */
    public static String getVersion(){
        return "3.2.0";
    }
}
