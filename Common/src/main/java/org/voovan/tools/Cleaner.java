package org.voovan.tools;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class Cleaner {
    private static ReferenceQueue<Object> refQueue = new ReferenceQueue<Object>();
    private static Map<Reference<Object>, Runnable> taskMap = new ConcurrentHashMap<Reference<Object>, Runnable>();

    // 清理线程
    private static class CleanerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Object object = refQueue.poll();
                    if(object==null){
                        TEnv.sleep(5);
                        continue;
                    }

                    Runnable runnable = taskMap.get(object);
                    taskMap.remove(object);
                    runnable.run();
                    System.out.println("========> cleaner");
                } catch (Exception e) {

                }

            }
        }
    }

    public Cleaner(Object object, Runnable runnable){
        PhantomReference<Object> reference = new PhantomReference<Object>(object, refQueue);
        taskMap.put(reference, runnable);
    }

    public static Cleaner register(Object object, Runnable runnable){
        return new Cleaner(object, runnable);
    }
}
