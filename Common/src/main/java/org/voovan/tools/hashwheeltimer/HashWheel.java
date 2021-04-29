package org.voovan.tools.hashwheeltimer;

import org.voovan.tools.collection.MultiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 时间轮对象
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HashWheel {
    private MultiMap<Integer, HashWheelTask> wheel;
    private int size = 0;
    private int currentSlot;
    private ReentrantLock lock;

    /**
     * 构造函数
     * @param size 时间轮的槽数
     */
    public HashWheel(int size){
        currentSlot = 0;
        this.size = size;
        wheel = new MultiMap<Integer, HashWheelTask>();
        lock = new ReentrantLock();
    }

    /**
     * 增加任务
     * @param task 任务对象
     * @param interval 任务间隔的槽数
     * @param asynchronous 是否异步执行
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task, int interval, boolean asynchronous){

        lock.lock();
        try {

            if (interval <= 0) {
                //这里考虑抛出异常
                return false;
            }

            final HashWheel innerHashWheel = this;

            int nextSlot = currentSlot + interval;

            int skipSlot = interval / size;

            //计算 SLot 位置
            int targetSlot = nextSlot % size;

            //对于步长等于槽数,的特殊处理
            if(interval%size == 0 && skipSlot > 0 && task.getDoCount() !=0 ){
                skipSlot--;
            }

//           Logger.simple("ST: "+skipSlot+" TT: "+targetSlot+ " CS:" +currentSlot + " I:" + interval);

            //重新安置任务
            wheel.putValue(targetSlot, task);

            task.init(skipSlot, interval, asynchronous, innerHashWheel, targetSlot);

            return true;
        }finally {
            lock.unlock();
        }
    }

    /**
     * 增加任务
     *      同步方式执行
     * @param task 任务对象
     * @param interval 任务间隔的槽数
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task, int interval){
        return addTask(task, task.getInterval(), false);
    }


    /**
     * 增加任务
     *
     *
     * @param task 任务对象
     * @return true 增加任务成功, false: 增加任务失败, 任务的Interval必须大于0
     */
    public boolean addTask(HashWheelTask task){
        return addTask(task, task.getInterval(), task.isAsynchronous());
    }

    /**
     * 移除任务
     * @param task 任务
     * @return true:移除任务成功, false:移除任务失败,或任务不存在
     */
    public boolean removeTask(HashWheelTask task){
        return wheel.removeValue(task.getSlot(), task);
    }

    /**
     * 清理所有的任务
     *        请注意系统任务也会被清理
     */
    public void clear(){
        wheel.clear();
    }

    /**
     * 执行一个步长
     */
    public void Tick(){
        lock.lock();
        try {

            if (currentSlot == size) {
                currentSlot = 0;
            }

            List<HashWheelTask> tasks = wheel.get(currentSlot);

            if (tasks != null) {
                for (HashWheelTask task : tasks.toArray(new HashWheelTask[tasks.size()])) {
                    if(task!=null) {
                        task.doTask();
                    } else {
                        tasks.remove(task);
                    }
                }
            }

            currentSlot++;
        } finally {
            lock.unlock();
        }

//        Logger.simple(currentSlot);

    }
}
