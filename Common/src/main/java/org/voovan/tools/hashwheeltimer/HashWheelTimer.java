package org.voovan.tools.hashwheeltimer;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 时间轮定时器
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HashWheelTimer {
    private HashWheel wheel;
    private int tickStep = 1;
    private Timer timer;

    /**
     * 构造函数
     *          默认每槽的步长是1s
     * @param size 时间轮的槽数
     */
    public HashWheelTimer(int size){
        wheel = new HashWheel(size);
    }

    /**
     * 构造函数
     * @param size 时间轮的槽数
     * @param tickStep 每槽的步长
     */
    public HashWheelTimer(int size, int tickStep){
        wheel = new HashWheel(size);
        this.tickStep = tickStep;
    }

    /**
     * 增加任务
     *      同步方式执行
     * @param task 任务对象
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task, int interval){
        return addTask(task, interval, false);
    }

    /**
     * 增加任务
     * @param task 任务对象
     * @param asynchronous 是否异步执行
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task, int interval, boolean asynchronous){
        return wheel.addTask(task, interval, asynchronous);
    }


    /**
     * 移除任务
     * @param tick 槽位号
     * @param task 任务
     * @return true:移除任务成功, false:移除任务失败,或任务不存在
     */
    public boolean removeTask(int tick, HashWheelTask task){
        return wheel.removeTask(tick, task);
    }


    /**
     * 启动时间轮的轮转
     * @return true:成功启动, false:时间轮已经启动
     */
    public boolean rotate(){
        if(timer != null){
            return false;
        }

        final HashWheel rotateWheel = wheel;
        timer = new Timer("VOOVAN@HASH_WHEEL");
        timer.schedule(new TimerTask(){

            @Override
            public void run() {
                rotateWheel.run();
            }
        }, 0, tickStep*1000);

        return true;
    }

    /**
     * 停止时间轮的轮转
     */
    public void cancel(){
        timer.cancel();
    }
}
