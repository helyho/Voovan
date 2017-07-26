package org.voovan.tools.hashwheeltimer;

import org.voovan.tools.MultiMap;
import org.voovan.tools.log.Logger;

import java.util.ArrayList;
import java.util.List;


/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HashWheel {
    private MultiMap<Integer, HashWheelTask> wheel;
    private int size = 0;
    private int currentTick;
    private boolean init;

    /**
     * 构造函数
     * @param size 时间轮的槽数
     */
    public HashWheel(int size){
        init = true;
        currentTick = 0;
        this.size = size;
        wheel = new MultiMap<Integer, HashWheelTask>();
    }

    /**
     * 增加任务
     * @param task 任务对象
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task, int interval){

        if(interval<0){
            //这里考虑抛出异常
            return false;
        }

        task.setInterval(interval);

        int nextTick = currentTick + task.getInterval();


        int skipTick = 0;

        //初始化或者相等情况不加跳跃数据
        if(nextTick == size && !init){
            skipTick = 0;
        }else{
            //计算跳跃次数
            skipTick = nextTick/size;
        }

        //计算 SLot 位置
        int targetTick = nextTick%size;

        //重置跳跃次数
        task.setSkipTick(skipTick);

        //重新安置任务
        wheel.putValue(targetTick, task);

        return true;
    }

    /**
     * 增加任务
     *
     * @param task 任务对象
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    private boolean addTask(HashWheelTask task){
        return addTask(task, task.getInterval());
    }

    /**
     * 移除任务
     * @param tick 槽位号
     * @param task 任务
     * @return true:移除任务成功, false:移除任务失败,或任务不存在
     */
    public boolean removeTask(int tick, HashWheelTask task){
        return wheel.removeValue(tick, task);
    }

    /**
     * 执行任务
     */
    public void run(){
        if(init) {
            init = false;
        }

        if(currentTick == size){
            currentTick = 0;
        }

        List<HashWheelTask> tasks = wheel.get(currentTick);

        if(tasks != null) {
            List<HashWheelTask> tmpList = new ArrayList<HashWheelTask>();
            tmpList.addAll(tasks);

            for (HashWheelTask task : tmpList) {
                if (task.doTask()) {
                    removeTask(currentTick, task);
                    addTask(task);
                }
            }
        }

        currentTick++;

        Logger.simple(currentTick);

    }
}
