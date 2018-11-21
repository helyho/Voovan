package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;

/**
 * Cleaner 非对内存释放类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Cleaner extends PhantomReference<Object> {
    private static final ReferenceQueue<Object> dummyQueue = new ReferenceQueue();
    private static Cleaner first = null;
    private Cleaner next = null;
    private Cleaner prev = null;
    private final Runnable thunk;

    static {
        Global.getHashWheelTimer().addTask(new HashWheelTask() {
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
        }, 1);
    }

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
