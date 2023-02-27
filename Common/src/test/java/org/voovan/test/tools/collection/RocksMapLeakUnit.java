package org.voovan.test.tools.collection;

import junit.framework.TestCase;
import org.rocksdb.*;
import org.rocksdb.util.BytewiseComparator;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TPerformance;
import org.voovan.tools.UniqueId;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.event.EventRunnerGroup;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.serialize.ProtoStuffSerialize;
import org.voovan.tools.serialize.TSerialize;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 类文字命名
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class RocksMapLeakUnit extends TestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TSerialize.SERIALIZE = new ProtoStuffSerialize();
    }

    public void testPut() {
        RocksMap<Integer, String> rocksMap = new RocksMap<>();

        Logger.info(TEnv.getCurrentPID());
        TEnv.sleep(1000);
        EventRunnerGroup eventRunnerGroup = EventRunnerGroup.newInstance(100);
        for(int p=0;p<1000;p++) {

            IntStream.range(0, 100).forEach(x->{
                eventRunnerGroup.addEvent(()->{
                    for(int i = 0;i<10000;i++) {
                        rocksMap.put(i + "_" + randomString(), randomString());
                    }
                });
            });



            eventRunnerGroup.process();
            eventRunnerGroup.await();

            System.gc();
            TEnv.sleep(2000);
            System.out.println("done ");
        }

        while(true) {
            TEnv.sleep(1000);
        }

    }

    public void testTransaction() {
        RocksMap<Integer, String> rocksMap = new RocksMap<>();

        Logger.info(TEnv.getCurrentPID());
        TEnv.sleep(1000);
        EventRunnerGroup eventRunnerGroup = EventRunnerGroup.newInstance(100);
        for(int p=0;p<1000;p++) {

            IntStream.range(0, 100).forEach(x->{
                eventRunnerGroup.addEvent(()->{
                    rocksMap.withTransaction(rdb->{
                        for(int i = 0;i<10000;i++) {
                            rocksMap.put(i + "_" + randomString(), randomString());
                        }
                        return true;
                    });


                });
            });

            eventRunnerGroup.process();
            eventRunnerGroup.await();

            rocksMap.flush();
            System.gc();
            TEnv.sleep(2000);
            Runtime runtime = Runtime.getRuntime();
            System.out.println("done " + rocksMap.estimateSize());

        }

        while(true) {
            TEnv.sleep(1000);
        }

    }


    public void testRemove() {
        RocksMap<Integer, String> rocksMap = new RocksMap<>();

        Logger.info(TEnv.getCurrentPID());
        TEnv.sleep(1000);
        EventRunnerGroup eventRunnerGroup = EventRunnerGroup.newInstance(100);
        for(int p=0;p<100;p++) {

            eventRunnerGroup.addEvent(()->{
                AtomicInteger  i= new AtomicInteger();
                rocksMap.scan(entry->{
                    entry.getKey();
                    entry.getValue();

                    entry.remove();

                    i.decrementAndGet();
                    if(i.get()%10000==0) {
                        System.out.println("->" + i.get());
                        return false;
                    }

                    return true;
                });


            });

            eventRunnerGroup.process();
            eventRunnerGroup.await();
            //for destory tomb stones
            rocksMap.compact();
            //for estimate size
            rocksMap.flush();
            System.gc();
            TEnv.sleep(2000);
            Runtime runtime = Runtime.getRuntime();
            System.out.println("done " + rocksMap.estimateSize() + " " + TPerformance.getJVMMemoryInfo().getHeapCommit()/1024/1024);

        }

        while(true) {
            TEnv.sleep(1000);
        }

    }



    public void testGet() {
        RocksMap<Integer, String> rocksMap = new RocksMap<>();

        Logger.info(TEnv.getCurrentPID());
        TEnv.sleep(1000);

        for(int p=0;p<1000;p++) {
            AtomicInteger  i= new AtomicInteger();
            rocksMap.scan(entry->{
                entry.getKey();
                entry.getValue();

                i.decrementAndGet();
                if(i.get()%1000000==0) {
                    System.out.println("->" + i.get());
                }

                return true;
            });


            System.gc();
            TEnv.sleep(2000);
            System.out.println("done");
        }

        while(true) {
            TEnv.sleep(1000);
        }

    }

    public void testWal() {
        RocksMap<Integer, String> rocksMap = new RocksMap<>();

        Logger.info(TEnv.getCurrentPID());
        TEnv.sleep(1000);
        for(int i=0;i<1000;i++) {

            List mm = rocksMap.getWalBetween(0L, 3000000L, null, true);

            mm.clear();

            System.gc();
            TEnv.sleep(2000);
            System.out.println("done");
        }

        while(true) {
            TEnv.sleep(1000);
        }

    }

    public String  randomString() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();
        String generatedString = random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        return generatedString;
    }

}