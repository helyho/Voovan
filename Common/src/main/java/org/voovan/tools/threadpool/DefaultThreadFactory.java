package org.voovan.tools.threadpool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DefaultThreadFactory implements ThreadFactory {
    private String name;
    private AtomicInteger threadCount = new AtomicInteger(0);
    private boolean daemon;
    private int priority;

    public DefaultThreadFactory(String name) {
        this(name, false, 5);
    }

    public DefaultThreadFactory(String name, boolean daemon) {
        this(name, daemon, 5);
    }

    public DefaultThreadFactory(String name, boolean daemon, int priority) {
        this.name = name;
        this.daemon = daemon;
        this.priority = priority;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, name + "-" + threadCount.getAndIncrement());

        try {
            if (thread.isDaemon()) {
                if (!this.daemon) {
                    thread.setDaemon(false);
                }
            } else if (this.daemon) {
                thread.setDaemon(true);
            }

            if (thread.getPriority() != this.priority) {
                thread.setPriority(this.priority);
            }
        } catch (Exception e) {
        }

        return thread;
    }
}
