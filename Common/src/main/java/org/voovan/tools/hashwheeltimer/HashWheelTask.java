package org.voovan.tools.hashwheeltimer;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class HashWheelTask {
    private int interval;
    private int skipTick = 0;

    public HashWheelTask(){
        this.interval = 0;
        this.skipTick = 0;
    }

    /**
     * 获取跳跃的 Tick 次数
     * @return 跳跃的 Tick 次数
     */
    public int getSkipTick() {
        return skipTick;
    }

    /**
     * 设置跳跃的 Tick 次数
     * @param skipTick 跳跃的 Tick 次数
     */
    public void setSkipTick(int skipTick) {
        this.skipTick = skipTick;
    }

    /**
     * 获取当前任务的时间间隔
     * @return 当前任务的时间间隔
     */
    public int getInterval() {
        return interval;
    }

    /**
     * 设置当前任务的时间间隔
     * @param interval 当前任务槽间隔,单位: 秒
     */
    protected void setInterval(int interval) {
        this.interval = interval;
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
        if(skipTick > 0){
            skipTick--;
            return false;
        }else {
            run();
            return true;
        }
    }
}
