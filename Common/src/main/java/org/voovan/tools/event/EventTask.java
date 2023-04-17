package org.voovan.tools.event;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * 时间任务
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventTask extends FutureTask implements Comparable {
    private int priority;

    public EventTask(int priority, Runnable runnable) {
        super(runnable, null);
        this.priority = priority;
    }

    public EventTask(int priority, Callable callable) {
        super(callable);
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int compareTo(Object o) {
        EventTask current = (EventTask) o;
        if (current.priority > this.priority) {
            return 1;
        } else if (current.priority == priority) {
            return 0;
        } else {
            return -1;
        }
    }

    public static EventTask newInstance(int priority, Runnable runnable) {
        return new EventTask(priority, runnable);
    }

    public static EventTask newInstance(int priority, Callable callable) {
        return new EventTask(priority, callable);
    }
}
