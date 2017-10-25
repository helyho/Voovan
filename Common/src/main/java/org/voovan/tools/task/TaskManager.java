package org.voovan.tools.task;


import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.util.List;
import java.util.Vector;

/**
 * 任务管理器
 *      异步的每隔一定的时间执行某个任务
 *
 * @author: helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public class TaskManager {
    private List<Task> taskList = new Vector<Task>();
    private Task[] taskArrayTemplate = new Task[0];
    /**
     * 增加任务
     * @param task 任务对象
     */
    public void addTask(Task task){
        taskList.add(task);
    }

    /**
     * 移除任务
     * @param task 任务对象
     */
    public void removeTask(Task task){
        taskList.remove(task);
    }

    /**
     * 获取任务列表
     * @return 任务列表
     */
    public List<Task> getTaskList(){
        return taskList;
    }

    /**
     * 开始扫描并执行任务
     */
    public void scanTask(){
        Global.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(TEnv.isMainThreadShutDown()){
                        break;
                    }

                    int tryStartTaskCount = 0;
                    for (Task task : taskList.toArray(taskArrayTemplate)) {
                        try {
                            if (task.isRunning()) {
                                continue;
                            }

                            //检查任务是否到启动时间
                            if (task.canRun()) {
                                task.setRunning(true);
                                Global.getThreadPool().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            task.start();
                                            task.setRunning(false);
                                        }
                                    }
                                );
                            }
                        }catch(Exception e){
                            Logger.error("Task scan error " , e);
                        }

                        tryStartTaskCount++;

                        //防止大量任务带来的高 CPU 负载
                        if(tryStartTaskCount >= 100) {
                            TEnv.sleep(1);
                            tryStartTaskCount = 0;
                        }
                    }

                    TEnv.sleep(1);
                }
            }
        });
    }
}
