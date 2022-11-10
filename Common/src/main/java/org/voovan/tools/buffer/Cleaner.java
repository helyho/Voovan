package org.voovan.tools.buffer;

import org.voovan.tools.TProperties;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Cleaner 非堆内存释放类
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Cleaner extends PhantomReference<Object> {
    private static final ReferenceQueue<Object> dummyQueue = new ReferenceQueue();
    private static Cleaner first = null;
    private Cleaner c_next = null;
    private Cleaner c_prev = null;
    private final Runnable thunk;
    private static final Timer timer;

    static {

        Integer noHeapReleaseInterval = TProperties.getInt("framework", "NoHeapReleaseInterval", 5);
        timer = new Timer("VOOVAN@Cleaner", true);

        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                while(true) {
                    Reference cleanerRef= dummyQueue.poll();
                    if(cleanerRef == null){
                        break;
                    } else if(cleanerRef instanceof Cleaner){
                        Cleaner cleaner = (Cleaner) cleanerRef;
                        cleaner.clean();
                    }
                }
            }
        }, 1000, noHeapReleaseInterval);
    }

    private static synchronized Cleaner add(Cleaner cleaner) {
        if (first != null) {
            cleaner.c_next = first;
            first.c_prev = cleaner;
        }

        first = cleaner;
        return cleaner;
    }

    private static synchronized boolean remove(Cleaner cleaner) {
        if (cleaner.c_next == cleaner) {
            return false;
        } else {
            if (first == cleaner) {
                if (cleaner.c_next != null) {
                    first = cleaner.c_next;
                } else {
                    first = cleaner.c_prev;
                }
            }

            if (cleaner.c_next != null) {
                cleaner.c_next.c_prev = cleaner.c_prev;
            }

            if (cleaner.c_prev != null) {
                cleaner.c_prev.c_next = cleaner.c_next;
            }

            cleaner.c_next = cleaner;
            cleaner.c_prev = cleaner;
            return true;
        }
    }


    private Cleaner(Object obj, Runnable thunk) {
        super(obj, dummyQueue);
        this.thunk = thunk;
    }



    public static Cleaner create(Object obj, Runnable thunk) {
        return thunk == null ? null : add(new Cleaner(obj, thunk));
    }


    public void clean() {
        if (remove(this)) {
            try {
                this.thunk.run();
            } catch (final Throwable var2) {
                System.out.println("Cleaner terminated abnormally");
            }
        }
    }
}
