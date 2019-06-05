package org.voovan.test.tools.collection;

import org.rocksdb.RocksDBException;
import org.voovan.Global;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.TEnv;
import org.voovan.tools.UniqueId;
import org.voovan.tools.collection.RocksMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 类文字命名
 *
 * @author: helyho
 * ignite-test Framework.
 * WebSite: https://github.com/helyho/ignite-test
 * Licence: Apache v2 License
 */
public class RocksMapBench {
    public static void main(String[] args) throws RocksDBException {
        int threadSize = 1;
        int loopSize = 50000;

        CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        UniqueId uniqueId = new UniqueId();

        RocksMap rocksMap = new RocksMap("bench", "benchCF");
        //Rocksdb 数据库配置

        AtomicInteger x2 = new AtomicInteger(0);

        System.out.println(
                TEnv.measureTime(() -> {
                    for (int i = 1; i < threadSize+1; i++) {
                        final int finalI = i;
                        Global.getThreadPool().execute(() -> {
                            Map<String, TestObject> data = new HashMap<>();
                            Integer basekey = finalI * 100000000;
                            for (int m = 0; m < loopSize; m++) {
                                basekey =  basekey + 1;
//                                            String key = (m/1000) + "_" + uniqueId.nextString() + "_" + finalI1;
                                String str = uniqueId.nextString();
                                //插入数据
//                                rocksMap.put( (finalI + "_" + basekey), new TestObject());
                                try {
                                    rocksMap.beginTransaction();
                                    rocksMap.put( (finalI + "_" + basekey), new TestObject());
                                    rocksMap.commit();
                                } catch (RocksDBException e) {
                                    e.printStackTrace();
                                }

//                                for(int t=0;t<10;t++) {
//                                    data.put((finalI + "_" + basekey), new TestObject());
//                                    basekey = basekey + 1;
//                                    x2.getAndIncrement();
//                                }
//                                rocksMap.putAll(data);
                                data.clear();
                            }

                            System.out.println("finished " + Thread.currentThread().getName());
                            countDownLatch.countDown();
                        });
                    }

                    while (countDownLatch.getCount() != 0) {
                        TEnv.sleep(1);
                    }

                }) / 1000000000f);

        System.out.println(rocksMap.size());
    }
}
