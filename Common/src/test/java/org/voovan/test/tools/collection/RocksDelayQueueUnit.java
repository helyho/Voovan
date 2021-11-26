package org.voovan.test.tools.collection;

import junit.framework.TestCase;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.UniqueId;
import org.voovan.tools.collection.RocksDelayQueue;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.collection.RocksQueue;
import org.voovan.tools.event.EventRunnerGroup;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.serialize.ProtoStuffSerialize;
import org.voovan.tools.serialize.TSerialize;

import java.util.concurrent.Delayed;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class RocksDelayQueueUnit extends TestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
        TSerialize.SERIALIZE = new ProtoStuffSerialize();
    }


    public void testRocksQueue() {
        String cfName = "testdb1000";
        RocksMap rocksMap = new RocksMap(cfName);
        RocksDelayQueue rocksQueue = new RocksDelayQueue(rocksMap, "testDelayQueue");
        rocksQueue.clear();

        rocksQueue.add(new Rdqo(-1, "asdfasdfasdf"));
        rocksQueue.add(new Rdqo(4, "asdfasdfasdf"));
        rocksQueue.add(new Rdqo(5, "asdfasdfasdf"));
        rocksQueue.add(new Rdqo(6, "asdfasdfasdf"));
        rocksQueue.add(new Rdqo(7, "asdfasdfasdf"));
        rocksQueue.add(new Rdqo(1, "asdfasdfasdf"));
        rocksQueue.add(new Rdqo(2, "asdfasdfasdf"));
        rocksQueue.add(new Rdqo(3, "asdfasdfasdf"));


        for(int i=0;i<10;i++) {
            if(i==3){
                rocksQueue.add(new Rdqo(0, "asdfasdfasdf1"));
                System.out.println(TDateTime.now() + " " + JSON.toJSON(rocksQueue.poll()));
            }
            System.out.println(TDateTime.now() + " " + JSON.toJSON(rocksQueue.poll()));
            TEnv.sleep(1000);
        }

        TEnv.sleep(1000);

    }

    public void testConcurrentRocksQueue() {
        String cfName = "testdb1000";
        RocksMap rocksMap = new RocksMap(cfName);
        RocksDelayQueue rocksQueue = new RocksDelayQueue(rocksMap, "testDelayQueue");
        rocksQueue.clear();
        System.out.println(rocksQueue.size());

        EventRunnerGroup eventRunnerGroup = EventRunnerGroup.newInstance();

        AtomicInteger pollCnt = new AtomicInteger();
        UniqueId uniqueId = new UniqueId();

//        TEnv.measure("add cost: ", ()->{
//            for (int x = 0; x < 50; x++) {
//                int finalX = x;
//                eventRunnerGroup.addEvent(() -> {
//                            for (int i = 0; i < 10000; i++) {
//                                rocksQueue.add(new Rdqo(i, uniqueId.nextNumber()));
//                            }
//                        }
//                );
//            }
//
//            eventRunnerGroup.await();
//        });
//
//
//        System.out.println(rocksQueue.size());
//
//        TEnv.measure("poll cost: ", ()->{
//            for (int x = 0; x < 50; x++) {
//                int finalX = x;
//                eventRunnerGroup.addEvent(() -> {
//                            for (int i = 0; i < 10000; i++) {
//                                Rdqo rdqo = (Rdqo) rocksQueue.poll();
//                                if(rdqo==null) {
//                                    i--;
//                                } else {
//                                    System.out.println(TDateTime.now() + " " + rdqo.value);
//                                    pollCnt.getAndIncrement();
//                                }
//                            }
//                        }
//                );
//            }
//
//            eventRunnerGroup.await();
//        });


        TEnv.measure("mix cost: ", ()->{
            for (int x = 0; x < 100; x++) {
                int finalX = x;
                eventRunnerGroup.addEvent(() -> {
                            for (int i = 0; i < 1000; i++) {
                                if(finalX%2 == 0) {
                                    rocksQueue.add(new Rdqo(i, uniqueId.nextNumber()));
                                } else {
                                    Rdqo rdqo = (Rdqo) rocksQueue.poll();
                                    if (rdqo == null) {
                                        TEnv.sleep(100);
                                        i--;
                                    } else {
                                        System.out.println(TDateTime.now() + " " + rdqo.value);
                                        pollCnt.getAndIncrement();
                                        i++;
                                    }
                                }
                            }
                        }
                );
            }
            eventRunnerGroup.await();
        });

        System.out.println(TDateTime.now() + " " + pollCnt.get());
        System.out.println(rocksQueue.size());

        TEnv.sleep(4000);
        assertEquals(rocksQueue.size(), 0);
    }
}
