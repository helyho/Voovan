package org.voovan.test.tools.collection;

import junit.framework.TestCase;
import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.UniqueId;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.collection.RocksQueue;
import org.voovan.tools.event.EventRunnerGroup;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.serialize.ProtoStuffSerialize;
import org.voovan.tools.serialize.TSerialize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class RocksQueueUnit extends TestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();
//        TSerialize.SERIALIZE = new ProtoStuffSerialize();
    }

    public void testRocksQueue() {

        String cfName = "testdb1000";
        RocksMap rocksMap = new RocksMap(cfName);
        RocksQueue rocksQueue = new RocksQueue(rocksMap, "testQueue");

        System.out.println(rocksQueue.toString());
        System.out.println(JSON.toJSON(rocksQueue.toArray()));
        for(int i=0;i<10;i++) {
            rocksQueue.add(i);
        }

        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        System.out.println(rocksQueue.toString());

        for(int i=0;i<3;i++) {
            System.out.println(rocksQueue.poll());
        }

        System.out.println(rocksQueue.toString());

        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        System.out.println("get " + rocksQueue.get(2));

        System.out.println("size: " + rocksQueue.size());
    }

    public void testRocksQueueTake() {
        String cfName = "testdb1000";
        RocksMap rocksMap = new RocksMap(cfName);
        RocksQueue rocksQueue = new RocksQueue(rocksMap, "testQueue");
        Global.getThreadPool().execute(()->{
            try {
                System.out.println("data: " + rocksQueue.take(1, TimeUnit.MINUTES));
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        });

        TEnv.sleep(2000);
        System.out.println("add");
        rocksQueue.add(1031);
        TEnv.sleep(100000);
    }

    public void testConcurrentRocksQueue() {
        String cfName = "testdb1000";
        RocksMap rocksMap = new RocksMap(cfName);
        RocksQueue rocksQueue = new RocksQueue(rocksMap, "testQueue");
        rocksQueue.clear();
        System.out.println(rocksQueue.toString());
        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        EventRunnerGroup eventRunnerGroup = EventRunnerGroup.newInstance();

        for(int x = 0;x<50;x++) {
            int finalX = x;
            eventRunnerGroup.addEvent(()->{
                    for (int i = 0; i < 1000; i++) {
                        rocksQueue.add(finalX *10 + i);
                    }
                }
            );
        }

        eventRunnerGroup.await();

        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        System.out.println(rocksQueue.toString() + " " + rocksQueue.size());

        for(int x = 0;x<50;x++) {
            int finalX = x;
            eventRunnerGroup.addEvent(()->{
                        for (int i = 0; i < 1000; i++) {
                            System.out.println(rocksQueue.poll());
                        }
                    }
            );
        }

        eventRunnerGroup.await();

        System.out.println(rocksQueue.toString());

        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        System.out.println("get " + rocksQueue.get(2));

        assertEquals(rocksQueue.size(), 0);
    }
}
