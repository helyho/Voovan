package org.voovan.tools;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class Cleaner extends PhantomReference<Object> {
    private static final ReferenceQueue<Object> dummyQueue = new ReferenceQueue();
    private static Cleaner first = null;
    private Cleaner next = null;
    private Cleaner prev = null;
    private final Runnable task;

    private static synchronized Cleaner add(Cleaner cleaner) {
        if (first != null) {
            cleaner.next = first;
            first.prev = cleaner;
        }

        first = cleaner;
        return cleaner;
    }

    private static synchronized boolean remove(Cleaner cleaner) {
        if (cleaner.next == cleaner) {
            return false;
        } else {
            if (first == cleaner) {
                if (cleaner.next != null) {
                    first = cleaner.next;
                } else {
                    first = cleaner.prev;
                }
            }

            if (cleaner.next != null) {
                cleaner.next.prev = cleaner.prev;
            }

            if (cleaner.prev != null) {
                cleaner.prev.next = cleaner.next;
            }

            cleaner.next = cleaner;
            cleaner.prev = cleaner;
            return true;
        }
    }

    private Cleaner(Object object, Runnable task) {
        super(object, dummyQueue);
        this.task = task;
    }

    public static Cleaner register(Object object, Runnable task) {
        return task == null ? null : add(new Cleaner(object, task));
    }

    public void clean() {
        if (remove(this)) {
            try {
                this.task.run();
            } catch (final Throwable var2) {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() {
                        if (System.err != null) {
                            (new Error("Cleaner terminated abnormally", var2)).printStackTrace();
                        }

                        System.exit(1);
                        return null;
                    }
                });
            }

        }
    }
}
