package org.voovan.test.tools.collection;

import org.rocksdb.RocksDBException;
import org.voovan.Global;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
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
        int threadSize = 4;
        int loopSize = 20000;
        int readConnt = 2;
        int updateConnt = 4;

        CountDownLatch countDownLatch = new CountDownLatch(threadSize);
        UniqueId uniqueId = new UniqueId();

        RocksMap rocksMap = new RocksMap("bench", "benchCF");
        //Rocksdb 数据库配置
        String lockkey1 = "lockkey1";
        rocksMap.put(lockkey1, 1);
        String lockkey2 = "lockkey2";
        rocksMap.put(lockkey2, 1);
        System.out.println("start...");

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
                                TestObject value = newone(key);
//                                String value = "gogogo!";

                                keys[x2.getAndIncrement()] = key;

                                //插入数据
                                rocksMap.put(key, value);

                                //随机读
                                {
                                    for (int k = 0; k < readConnt; k++) {
                                        int index = (int) (Math.random() * x2.get()-1);
                                        rocksMap.get(keys[index]);
                                    }
                                }
//
//                                //随机更新
//                                {
//                                    for (int k = 0; k < updateConnt; k++) {
//                                        int index = (int) (Math.random() * x2.get()-1);
//                                        rocksMap.put(keys[index], value);
//                                    }
//                                }

                                //事务
                                {
                                    rocksMap.beginTransaction();
                                    //锁竞争
//                                    int x = (Integer) rocksMap.lock(lockkey1);
                                    int x = (int) rocksMap.get(lockkey1);
                                    rocksMap.put(lockkey1, x + 1);
//                                    int y = (Integer) rocksMap.lock(lockkey2);
                                    int y = (int) rocksMap.get(lockkey2);
                                    rocksMap.put(lockkey2, y + 1);

//                                    rocksMap.put(key, value);
                                    rocksMap.commit();
                                }

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

                            rocksMap.flush();
                            System.out.println("finished " + Thread.currentThread().getName());
                            countDownLatch.countDown();
                        });
                    }

                    while (countDownLatch.getCount() != 0) {
                        TEnv.sleep(1);
                    }

                }) / 1000000000f);

        System.out.println(rocksMap.get(lockkey1));
        System.out.println(rocksMap.get(lockkey2));
        System.out.println(rocksMap.size());
    }


    public static TestObject newone(String name){
        TestObject testObject = new TestObject();
        int random = (int) (Math.random() * 800000);
        testObject.setString(name);
        testObject.setBint(random);
        testObject.getList().add("listitem1" +random);
        testObject.getList().add("listitem2 " +random);
        testObject.getList().add("listitem3" +random);
//        testObject.getMap().put("mapitem1" +random, "mapitem1" +random);
//        testObject.getMap().put("mapitem2" +random, "mapitem2" +random);
        testObject.getTb2().setString("bingo");
        testObject.getTb2().setBint(56);
        testObject.getTb2().getList().add("tb2 list item");
//        testObject.getTb2().getMap().put("tb2 map item", "tb2 map item");

        return testObject;
    }
}
