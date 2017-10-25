package org.voovan.test.tools.task;

import org.voovan.Global;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;
import org.voovan.tools.task.Task;
import org.voovan.tools.task.TaskManager;

/**
 * TaskExample使用用例
 *
 * @author: helyho
 * Framework Framework.
 * WebSite: https://github.com/helyho/Framework
 * Licence: Apache v2 License
 */
public class TaskExample {
    public static void main(String[] args) {
        TaskManager taskManager = Global.getTaskManager();

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
