package org.voovan.tools.hashwheeltimer;

import org.voovan.Global;

/**
 * 时间轮任务对象
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class HashWheelTask {
    private int interval;
    private int skipTick = 0;
    private boolean asynchronous;
    private HashWheel hashWheel;
    private int slot;
    private long doCount;
    private boolean isCancel;

    /**
     * 构造函数
     */
    public HashWheelTask(){
        this.interval = 0;
        this.skipTick = 0;
        this.asynchronous=false;
        this.isCancel = false;
    }


    /**
     * 构造函数
     * @param interval      任务的槽间隔数
     * @param asynchronous  是否异步执行
     */
    public HashWheelTask(int interval, boolean asynchronous){
        this.interval = interval;
        this.skipTick = 0;
        this.asynchronous=asynchronous;
        this.isCancel = false;
    }


    protected void init(int skipTick, int interval, boolean asynchronous,  HashWheel hashWheel, int slot){
        this.skipTick = skipTick;
        this.interval = interval;
        this.asynchronous = asynchronous;
        this.hashWheel = hashWheel;
        this.slot = slot;
        doCount = 0;
        this.isCancel = false;
    }

    /**
     * 判断任务是否已经取消
     * @return true:已经取消, false: 未取消
     */
    public boolean isCancel() {
        return isCancel;
    }

    /**
     * 获取跳跃的轮次数
     * @return 跳跃的轮次数
     */
    public int getSkipTick() {
        return skipTick;
    }

    /**
     * 设置跳跃的轮次数
     * @param skipTick 跳跃的轮次数
     */
    public void setSkipTick(int skipTick) {
        this.skipTick = skipTick;
    }

    /**
     * 获取当前任务的槽间隔
     * @return 当前任务的槽间隔
     */
    public int getInterval() {
        return interval;
    }

    /**
     * 设置当前任务的槽间隔
     * @param interval 当前任务槽间隔,单位: 秒
     */
    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * 是否是异步任务
     * @return true: 异步任务, false: 同步任务
     */
    public boolean isAsynchronous() {
        return asynchronous;
    }

    /**
     * 设置是否是异步任务
     * @param asynchronous true: 异步任务, false: 同步任务
     */
    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    /**
     * 获取当前任务的槽位
     * @return 当前任务的槽位
     */
    public int getSlot() {
        return slot;
    }

    /**
     * 获取任务执行的次数
     * @return 任务执行的次数
     */
    public long getDoCount() {
        return doCount;
    }

    /**
     * 取消当前任务
     * @return true: 成功, false:失败
     */
    public boolean cancel(){
        this.isCancel = true;
        return hashWheel.removeTask(this);
    }

    /**
     * 运行 Task
     */
    public abstract void run();

    /**
     * 步进一次
     * @return true: 执行了这个任务, false: 未执行这个任务
     */
    public boolean doTask(){
        doCount++;

        if(doCount == Long.MAX_VALUE){
            doCount = 0;
        }

        if(skipTick > 0){
            skipTick--;
            return false;
        }else {
            if(asynchronous){
                final HashWheelTask task = this;
                Global.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        task.run();
                    }
                });
            }else{
                run();
            }

            return true;
        }
    }
}
