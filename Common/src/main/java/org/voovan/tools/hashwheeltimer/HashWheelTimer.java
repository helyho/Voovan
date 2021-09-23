package org.voovan.tools.hashwheeltimer;

import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.log.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 时间轮定时器
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HashWheelTimer {
    private static AtomicInteger count = new AtomicInteger(0);
    private HashWheel wheel;
    private int tickStep = 1000;
    private Timer timer;
    private String name;

    /**
     * 构造函数
     *          默认每槽的步长是1s
     * @param size 时间轮的槽数
     */
    public HashWheelTimer(int size){
        this(null, size, -1);
    }

    /**
     * 构造函数
     * @param size 时间轮的槽数
     * @param tickStep 每槽的步长, 单位: 毫秒
     */
    public HashWheelTimer(int size, int tickStep) {
        this(null, size, tickStep);
    }

    /**
     * 构造函数
     *          默认每槽的步长是1s
     * @param name 时间轮定时器名称
     * @param size 时间轮的槽数
     */
    public HashWheelTimer(String name, int size){
        this(name, size, -1);
    }

    /**
     * 构造函数
     * @param name 时间轮定时器名称
     * @param size 时间轮的槽数
     * @param tickStep 每槽的步长, 单位: 毫秒
     */
    public HashWheelTimer(String name, int size, int tickStep){
        this.name = TObject.nullDefault(name, count.getAndIncrement()+"");
        wheel = new HashWheel(size);
        timer = new Timer("WHEEL_TIMER_" + this.name, true);
        this.tickStep = tickStep!=-1 ? tickStep : this.tickStep;
    }

    /**
     * 增加任务
     *      同步方式执行
     * @param task 任务对象
     * @param interval 任务间隔的槽数
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task, int interval){
        return addTask(task, interval, false);
    }

    /**
     * 增加任务
     * @param task 任务对象
     * @param interval 任务间隔的槽数
     * @param asynchronous 是否异步执行
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task, int interval, boolean asynchronous){
        return wheel.addTask(task, interval, asynchronous);
    }

    /**
     * 增加任务
     *      同步方式执行
     * @param task 任务对象
     * @param interval 任务间隔的槽数
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(Runnable task, int interval){
        return addTask(HashWheelTask.newInstance(task), interval, false);
    }

    /**
     * 增加任务
     * @param task 任务对象
     * @param interval 任务间隔的槽数
     * @param asynchronous 是否异步执行
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(Runnable task, int interval, boolean asynchronous){
        return wheel.addTask(HashWheelTask.newInstance(task), interval, asynchronous);
    }


    /**
     * 移除任务
     * @param task 任务
     * @return true:移除任务成功, false:移除任务失败,或任务不存在
     */
    public boolean removeTask(HashWheelTask task){
        return wheel.removeTask(task);
    }


    /**
     * 启动时间轮的轮转
     * @return true:成功启动, false:时间轮已经启动
     */
    public boolean rotate(){

        final HashWheel rotateWheel = wheel;

        final HashWheelTimer tempTimer = this;

        timer.schedule(new TimerTask(){

            @Override
            public void run() {
                try {
                    rotateWheel.Tick();
                } catch (Exception e){
                    Logger.error("HashWheelTimer tick task error", e);
                }

                //如果进程结束自动结束当前定时器
                if(TEnv.IS_SHUTDOWN){
                    tempTimer.cancel();
                }
            }
        }, 0, tickStep);

        return true;
    }

    /**
     * 清理所有的任务
     *        请注意系统任务也会被清理
     */
    public void clear(){
        wheel.clear();
    }

    /**
     * 停止时间轮的轮转
     */
    public void cancel(){
        timer.cancel();
    }
}
