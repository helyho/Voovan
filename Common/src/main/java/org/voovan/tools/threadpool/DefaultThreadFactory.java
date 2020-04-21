package org.voovan.tools.threadpool;

import org.voovan.tools.FastThread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认线程工程类
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
        FastThread fastThread = new FastThread(r, name + "-" + threadCount.getAndIncrement());

        try {
            if (fastThread.isDaemon()) {
                if (!this.daemon) {
                    fastThread.setDaemon(false);
                }
            } else if (this.daemon) {
                fastThread.setDaemon(true);
            }

            if (fastThread.getPriority() != this.priority) {
                fastThread.setPriority(this.priority);
            }
        } catch (Exception e) {
        }

        return fastThread;
    }
}
