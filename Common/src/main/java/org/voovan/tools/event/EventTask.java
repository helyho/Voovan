package org.voovan.tools.event;

/**
 * 时间任务
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventTask implements Comparable {
    private int priority;
    private Runnable runnable;

    public EventTask(int priority, Runnable runnable) {
        this.priority = priority;
        this.runnable = runnable;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
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

    public static EventTask newInstance(Runnable runnable) {
        return new EventTask(0, runnable);
    }
}
