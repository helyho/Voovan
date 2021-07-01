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

    public void testConcurrentRocksQueue() {
        String cfName = "testdb1000";
        RocksMap rocksMap = new RocksMap(cfName);
        RocksQueue rocksQueue = new RocksQueue(rocksMap, "testQueue");
        System.out.println(rocksQueue.toString());
        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        for(int x = 0;x<5;x++) {
            int finalX = x;
            Global.getThreadPool().execute(()->{
                    for (int i = 0; i < 10; i++) {
                        rocksQueue.add(finalX *10 + i);
                    }
                }
            );
        }

        TEnv.sleep(1000);

        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        System.out.println(rocksQueue.toString());

        for(int x = 0;x<5;x++) {
            int finalX = x;
            Global.getThreadPool().execute(()->{
                        for (int i = 0; i < 10; i++) {
                            System.out.println(rocksQueue.poll());
                        }
                    }
            );
        }

        TEnv.sleep(1000);

        System.out.println(rocksQueue.toString());

        System.out.println(JSON.toJSON(rocksQueue.toArray()));

        System.out.println("get " + rocksQueue.get(2));

        System.out.println("size: " + rocksQueue.size());
    }
}
