package org.voovan.tools.task;

import org.voovan.tools.log.Logger;

/**
 * 任务对象
 *
 * @author: helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public abstract class Task {
    private String name;
    private int delay;
    private Thread currentThread;
    private long startTime;
    private boolean isRunning;
    private TaskManager taskManager;
    private long runCount;

    /**
     * 构造函数
     */
    public Task(){
        startTime = System.currentTimeMillis();
        isRunning = false;
    }

    /**
     * 构造函数
     * @param name 任务名称
     * @param delay 任务重复执行间隔事件
     */
    public Task(String name, int delay){
        startTime = System.currentTimeMillis();
        isRunning = false;
        init(name, delay);
    }


    /**
     * 初始化
     * @param name  任务名称
     * @param delay 任务重复执行间隔事件
     */
    protected void init(String name, int delay){
        if(runCount == 0) {
            this.name = name;
            this.delay = delay;
        }
    }

    /**
     * 获取任务名称
     * @return 任务名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置任务名称
     * @param name 任务名称
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取任务重复执行间隔时间, 单位: ms
     * @return  任务重复执行间隔时间
     */
    public int getDelay() {
        return delay;
    }

    /**
     * 获取任务重复执行间隔时间, 单位: ms
     * @param delay 任务重复执行间隔时间
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * 获取当前线程
     * @return 当前线程
     */
    public Thread getCurrentThread() {
        return currentThread;
    }


    /**
     * 获取任务执行次数
     * @return 任务的执行次数
     */
    public long getRunCount() {
        return runCount;
    }

    /**
     * 设置任务的执行状态
     * @param running true: 执行中, false: 停止执行
     */
    public void setRunning(boolean running) {
        isRunning = running;
    }

    /**
     * 任务是否正在执行
     * @return true: 正在执行, false: 任务停止了
     */
    public boolean isRunning(){
        return isRunning;
    }

    /**
     * 任务是否可执行
     * @return true: 任务可以执行, false: 任务处于等待事件
     */
    public boolean canRun(){
        if(System.currentTimeMillis() >= startTime){
            return true;
        }else{
            return false;
        }
    }

    /**
     * 抽象函数,任务业务逻辑实现在此
     */
    public abstract void run();

    /**
     * 启动任务
     */
    public void start(){
        try{
            this.currentThread = Thread.currentThread();

            run();

            //更新启动时间, 下次这个时间启动
            startTime = System.currentTimeMillis() + delay;
            if(runCount == Long.MAX_VALUE){
                runCount = 0;
            }
            runCount++;

        } catch (Exception e) {
            Logger.error("Task " + this.getName() + " has error: ", e);
        }
    }
}
