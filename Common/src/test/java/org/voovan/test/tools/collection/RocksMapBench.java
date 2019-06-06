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
        int threadSize = 8;
        int loopSize = 5000;
        int readConnt = 10;
        int updateConnt = 2;

        CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        UniqueId uniqueId = new UniqueId();

        RocksMap rocksMap = new RocksMap("bench", "benchCF");
        //Rocksdb 数据库配置
        String lastKey = "lastkey";
        rocksMap.put(lastKey, 1);

        AtomicInteger x2 = new AtomicInteger(0);

        String keys[] = new String[threadSize * loopSize + 10];

        System.out.println(
                TEnv.measureTime(() -> {
                    for (int i = 1; i < threadSize+1; i++) {
                        final int finalI = i;
                        Global.getThreadPool().execute(() -> {
                            Map<String, TestObject> data = new HashMap<>();
                            Integer basekey = finalI * 100000000;
                            for (int m = 0; m < loopSize; m++) {
                                basekey =  basekey + 1;
                                String key = finalI + "_" + basekey;
                                TestObject value = new TestObject();

                                keys[x2.getAndIncrement()] = key;

                                //插入数据
//                                rocksMap.put(key, value);

                                //随机读
                                {
                                    for (int k = 0; k < readConnt; k++) {
                                        int index = (int) (Math.random() * x2.get());
                                        rocksMap.get(keys[index]);
                                    }
                                }

//                                随机更新
                                {
                                    for (int k = 0; k < updateConnt; k++) {
                                        int index = (int) (Math.random() * x2.get());
                                        rocksMap.put(keys[index], value);
                                    }
                                }

                                //事务
//                                {
//                                    rocksMap.beginTransaction();
//                                    //锁
//                                    if(finalI%4 == 0){
//                                        int x = (Integer) rocksMap.getForUpdate(lastKey);
//                                        rocksMap.put(lastKey, x + 1);
//                                    }
//                                    rocksMap.put(key, value);
//                                    rocksMap.commit();
//                                }

                                //批量写入
                                {
//                                for(int t=0;t<10;t++) {
//                                    data.put(key+"_t", new TestObject());
//                                    basekey = basekey + 1;
//                                    x2.getAndIncrement();
//                                }
//                                rocksMap.putAll(data);
//                                data.clear();
                                }
                            }

                            System.out.println("finished " + Thread.currentThread().getName());
                            countDownLatch.countDown();
                        });
                    }

                    while (countDownLatch.getCount() != 0) {
                        TEnv.sleep(1);
                    }

                }) / 1000000000f);

        System.out.println(rocksMap.get(lastKey));
        System.out.println(rocksMap.size());
    }
}
