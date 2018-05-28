package org.voovan.test.tools.task;

import junit.framework.TestCase;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.task.Task;
import org.voovan.tools.task.TaskManager;

/**
 * 类文字命名
 *
 * @author: helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public class TaskUnit extends TestCase {
    private TaskManager taskManager;

    public void setUp(){
        taskManager = new TaskManager();
        taskManager.scanTask();
    }

    public void testAddTask(){

        for(int i=0;i<10;i++) {
            final int k  = i;
            taskManager.addTask(new Task() {
                @Override
                public void run() {
                    init("MyTask"+k, 1000);
                    Logger.simple(TDateTime.now() + " "+this.getName());
                    TEnv.sleep(2000);
                }
            });
        }

        TEnv.sleep(60*1000);
    }
}
