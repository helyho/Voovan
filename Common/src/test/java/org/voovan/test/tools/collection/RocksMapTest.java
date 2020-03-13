package org.voovan.test.tools.collection;

import junit.framework.TestCase;
import org.rocksdb.*;
import org.rocksdb.util.BytewiseComparator;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.UniqueId;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.json.JSON;
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
public class RocksMapTest extends TestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        TSerialize.SERIALIZE = new ProtoStuffSerialize();
    }

    public void testComparatorBench() {

        Options options = new Options();
        options.useCappedPrefixExtractor(8);
        DBOptions dbOptions = new DBOptions();
        ReadOptions readOptions = new ReadOptions();
        WriteOptions writeOptions = new WriteOptions();

        ColumnFamilyOptions cppColumnFamilyOptions = new ColumnFamilyOptions();
        cppColumnFamilyOptions.setComparator(new BytewiseComparator(new ComparatorOptions()));

        ColumnFamilyOptions javaColumnFamilyOptions = new ColumnFamilyOptions();
        Comparator comparator = new RocksComparatorTest(new ComparatorOptions());
        javaColumnFamilyOptions.setComparator(comparator);

        dbOptions.setCreateIfMissing(true);
        dbOptions.setCreateMissingColumnFamilies(true);
        dbOptions.setUseDirectReads(true);

        UniqueId uniqueId = new UniqueId(200, 10);

        RocksMap rocksMap2 = new RocksMap("javaComparator", "Default", cppColumnFamilyOptions, dbOptions, readOptions, writeOptions, false);


//        TEnv.measure("cppComparator", ()->{
//            for (int i = 0; i < 3000; i++) {
//                rocksMap2.put(((Long)uniqueId.nextNumber()).toString(), i);
//            }
//        });
        //                6642070616671666176
        rocksMap2.scan(null, "6642070616667471", entry->{
            System.out.println(((RocksMap.RocksMapEntry)entry).getKey());
            return true;
        }, true);

//        RocksMap rocksMap1 = new RocksMap("javaComparator", "Default", javaColumnFamilyOptions, dbOptions, readOptions, writeOptions, false);
//
//        TEnv.measure("javaComparator", ()->{
//            for (int i = 0; i < 3000; i++) {
//                rocksMap1.put(uniqueId.nextNumber(), i);
//            }
//        });

        System.out.println(1);
    }

    public void testWal() throws RocksDBException {
        DBOptions dbOptions = new DBOptions();
        ReadOptions readOptions = new ReadOptions();
        WriteOptions writeOptions = new WriteOptions();
        ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions();

//        dbOptions.setDbWriteBufferSize(1024*1024*512);

        dbOptions.setCreateIfMissing(true);
        dbOptions.setCreateMissingColumnFamilies(true);
        dbOptions.setUseDirectReads(true);
//        dbOptions.setUseDirectIoForFlushAndCompaction(true);

        //设置 MemTable 大小
//        columnFamilyOptions.setWriteBufferSize(1024*1024*512);

        RocksMap rocksMap =  new RocksMap("waltest", "cfname", columnFamilyOptions, dbOptions, readOptions, writeOptions, false);
        for(int i=0;i<300000;i++){
            rocksMap.put(i, i);
        }
        rocksMap.compact();

        rocksMap.remove(1);
        rocksMap.put(9, 65536);
        rocksMap.remove(9);
        HashMap m = new HashMap();
        m.put(11, 111);
        m.put(12, 122);
        m.put(13, 133);
        rocksMap.putAll(m);

        rocksMap.withTransaction((map)->{
            ((RocksMap)map).put(10, 31);
            return true;
        });

        rocksMap.withTransaction((map)->{
            ((RocksMap)map).put(15, 31);
            ((RocksMap)map).put(16, 32);
            return false;
        });
//
        rocksMap.choseColumnFamily("default").put(90, 65536);
        System.out.println(rocksMap.choseColumnFamily("cfname").get(90));

        List<RocksMap.RocksWalRecord> rocksWalRecords = rocksMap.getWalSince(0l, true);
        for(RocksMap.RocksWalRecord rocksWalRecord : rocksWalRecords) {

            System.out.println(rocksWalRecord.getSequence() + " " + rocksWalRecord.getType() + " " + rocksWalRecord.getColumnFamilyId() + " = " + rocksWalRecord.getChunks());
        }

        System.out.println("===============filter==================");
        rocksWalRecords = rocksMap.getWalSince(0l, (cfid, type)-> cfid.equals(5), true);
        for(RocksMap.RocksWalRecord rocksWalRecord : rocksWalRecords) {

            System.out.println(rocksWalRecord.getSequence() + " " + rocksWalRecord.getType() + " " + rocksWalRecord.getColumnFamilyId() + " = " + rocksWalRecord.getChunks());
        }

        System.out.println(rocksMap.entrySet());
    }

    public void testRemoveEmpty() throws RocksDBException {
        String cfName = "testdb_re";
        ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions();
        columnFamilyOptions.setCompactionFilter(new RemoveEmptyValueCompactionFilter());
        RocksMap rocksMap1 = new RocksMap(null, cfName, columnFamilyOptions, null, null, null, false);
        rocksMap1.put("1111", new byte[]{(byte)1, (byte)2});
        rocksMap1.put("2222", new byte[]{});
        System.out.println(rocksMap1.get("2222"));
        rocksMap1.compact();
        System.out.println(rocksMap1.get("2222"));
    }

    public void testAll() throws RocksDBException {
        //测试列族区分,这个列族不写入任何数据
        String cfName = "testdb1000";
        RocksMap rocksMap1 = new RocksMap(cfName);
        RocksMap rocksMapx = new RocksMap(cfName);
        if(rocksMap1.get("name") == null){
            System.out.println("putIfAbsent: "+ rocksMap1.putIfAbsent("name", cfName));
            System.out.println("putIfAbsent: "+ rocksMap1.putIfAbsent("name", cfName));

            System.out.println("replace: "+ rocksMap1.replace("name1", cfName));
            System.out.println("replace: "+ rocksMap1.replace("name1", cfName));
        }

        RocksMap rocksMap = new RocksMap("testdb");
        System.out.println(rocksMap.get("name"));
        rocksMap.put("11", "11");

        rocksMap.clear();

        System.out.println("isEmpty: " + rocksMap.isEmpty());
        System.out.println("size: " + rocksMap.size());

        //commit
        System.out.println("===============commit==================");
        rocksMap.beginTransaction();
        rocksMap.choseColumnFamily("testdb");
        rocksMap.put("transaction11", "rocksMap.value");
        rocksMap.choseColumnFamily(cfName);
        rocksMap.put("transaction11", cfName);

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap  get: "+ rocksMap.get("transaction11"));
        rocksMap.choseColumnFamily(cfName);
        System.out.println("rocksMap1 get: "+ rocksMap.get("transaction11"));
        rocksMap.choseColumnFamily("testdb");
        rocksMap.commit();

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap commit  get: "+ rocksMap.get("transaction11"));
        rocksMap.choseColumnFamily(cfName);
        System.out.println("rocksMap1 commit get: "+ rocksMap.get("transaction11"));
        rocksMap.choseColumnFamily("testdb");


        //rollback
        System.out.println("===============rollback==================");
//        rocksMap.choseColumnFamily("testdb");
        rocksMap.beginTransaction();
        rocksMap.put("transaction00", "rocksMap.value");
        rocksMap.choseColumnFamily(cfName);
        rocksMap.put("transaction00", cfName);

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap  get: "+ rocksMap.get("transaction00"));
        rocksMap.choseColumnFamily(cfName);
        System.out.println("rocksMap1 get: "+ rocksMap.get("transaction00"));
        rocksMap.rollback();

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap rollback get: "+ rocksMap.get("transaction00"));
        rocksMap.choseColumnFamily(cfName);
        System.out.println("rocksMap1 rollback get: "+ rocksMap1.get("transaction00"));



        System.out.println("===============withTransaction commit==================");
        //测试事务隔离
        rocksMap.beginTransaction();
        rocksMap.withTransaction(map ->{
            RocksMap rocksMapT = (RocksMap)map;
            rocksMapT.put("ddddk", "ffdasf");
            //测试事务隔离
            rocksMap.rollback();
            System.out.println("withTransaction: " + rocksMapT.get("ddddk"));
            return true;
        });

        System.out.println("withTransaction commit: " + rocksMap.get("ddddk"));
        rocksMap.remove("ddddk");

        //rollback
        System.out.println("===============withTransaction rollback ==================");
        //测试事务隔离
        rocksMap.beginTransaction();
        rocksMap.withTransaction(map ->{
            RocksMap rocksMapT = (RocksMap) map;
            rocksMapT.put("ddddk", "ffdasf");
            //测试事务隔离
            rocksMap.commit();
            System.out.println("withTransaction: " + rocksMapT.get("ddddk"));
            return false;
        });
        System.out.println("withTransaction rollback: " + rocksMap.get("ddddk"));

        //commit
        System.out.println("===============share commit ==================");
        rocksMap.choseColumnFamily("testdb");
        rocksMap.beginTransaction();
        rocksMap.put("transaction33", "rocksMap.value");
        RocksMap rocksMap2 = rocksMap.duplicate(cfName);
        rocksMap2.put("transaction33", cfName);
        rocksMap.commit();

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap share get: "+ rocksMap.get("transaction33"));
        System.out.println("rocksMap2 share get: "+ rocksMap2.get("transaction33"));
        rocksMap.choseColumnFamily("testdb");

        //rollback
        System.out.println("===============share rollback==================");
        rocksMap.beginTransaction();
        rocksMap.put("transaction22", "rocksMap.value");
        rocksMap2 = rocksMap.duplicate(cfName);
        rocksMap2.put("transaction22", cfName);

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap get: "+ rocksMap.get("transaction22"));
        System.out.println("rocksMap2 get: "+ rocksMap2.get("transaction22"));
        rocksMap.rollback();

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap share rollback get: "+ rocksMap.get("transaction22"));
        System.out.println("rocksMap2 share rollback get: "+ rocksMap2.get("transaction22"));
        System.out.println("=======================================");

        System.out.println("===============nest==================");
        rocksMap.beginTransaction();
        rocksMap.put("transaction44", "rocksMap.value");
        {
            rocksMap.beginTransaction();
            rocksMap.put("transaction55", "rocksMap.value");
            rocksMap.rollback();
        }
        rocksMap.commit();
        System.out.println("rocksMap nest commit get: "+ rocksMap.get("transaction44"));
        System.out.println("rocksMap2 nest rollback get: "+ rocksMap2.get("transaction55"));


        //put
        rocksMap.put("aaaa", "aaaa");
        rocksMap.put("bbbb", "bbbb");
        rocksMap.put("cccc", "cccc");
        rocksMap.put("dddd", "dddd");
        rocksMap.put("eeee", "eeee");
        rocksMap.put("ffff", "ffff");
        rocksMap.put("hhhh", "hhhh");
        rocksMap.put("iiii", "iiii");
        rocksMap.put("1111", "1111");
        rocksMap.put("2222", "2222");
        rocksMap.put("3333", "3333");
        rocksMap.put("4444", "4444");

        System.out.println("===============removeRange==================");
        System.out.println("removeRange: " + rocksMap.entrySet());
        rocksMap.removeRange("aaaa", "transact");
        System.out.println("removeRange: " + rocksMap.entrySet());


        //get
        System.out.println("get aaaa: "+ rocksMap.get("aaaa"));
        System.out.println("size: "+ rocksMap.size());

        //remove
        System.out.println("remove cccc: "+ rocksMap.remove("cccc"));
        System.out.println("get cccc: "+ rocksMap.get("cccc"));
        System.out.println("remove cccc: "+ rocksMap.remove("cccc"));
        System.out.println("size: "+ rocksMap.size());

        //keySet
        System.out.println("isEmpty: " + rocksMap.isEmpty());
        System.out.println("KeySet: "+ rocksMap.keySet());
        System.out.println(rocksMap.entrySet());

        System.out.println("=================iterator================");
        RocksMap.RocksMapIterator iterator = rocksMap.iterator();
        while(iterator.hasNext()){
            iterator.next();
            System.out.println(iterator.key() + " " + iterator.value());
        }

        System.out.println("=================iterator size 3================");
        iterator = rocksMap.iterator(3);
        while(iterator.hasNext()){
            iterator.next();
            System.out.println(iterator.key() + " " + iterator.value());
        }
        System.out.println("=================iterator remove================");
        iterator = rocksMap.iterator();
        iterator.remove();

        iterator = rocksMap.iterator();
        while(iterator.hasNext()){
            iterator.next();
            System.out.println(iterator.key() + " " + iterator.value());
        }
        System.out.println("=================iterator range 3333->transaction================");
        iterator = rocksMap.iterator("3", "tr");
        while(iterator.hasNext()) {
            iterator.next();
            System.out.println(iterator.key() + " " + iterator.value());
        }
        System.out.println("=================================================");

        System.out.println("=================iterator range 3333->end================");
        iterator = rocksMap.iterator("3333", null);//"eeee", "transaction11"
        while(iterator.hasNext()) {
            iterator.next();
            System.out.println(iterator.key() + " " + iterator.value());
        }
        System.out.println("=================================================");

        System.out.println("=================iterator range start->3333================");
        iterator = rocksMap.iterator(null, "3333");
        while(iterator.hasNext()) {
            iterator.next();
            System.out.println(iterator.key() + " " + iterator.value());
        }
        System.out.println("=================================================");

        //clear
        rocksMap.clear();
        System.out.println("size: "+ rocksMap.size());
        System.out.println("clear get: " + rocksMap.get("aaaa"));

        //putAll
        Map<String, String> data = new HashMap<>();
        data.put("hhhh1", "iiii");
        data.put("hhhh2", "iiii");
        data.put("hhhh3", "iiii");
        data.put("hhhh4", "iiii");
        data.put("hhhh5", "iiii");
        data.put("hhhh6", "iiii");
        data.put("hhhh7", "iiii");
        data.put("xxxx7", "iiii");
        rocksMap.putAll(data);
        System.out.println("size: "+ rocksMap.size());
        System.out.println("KeySet: "+ rocksMap.keySet());

        //submap
        Map subMap = rocksMap.subMap("hhhh2", "hhhh5");
        System.out.println("subMap 2->5: " + subMap);


        //headmap
        Map headMap = rocksMap.headMap("hhhh4");
        System.out.println("headMap b->4: " + headMap);

        //tailmap
        Map tailMap = rocksMap.tailMap("hhhh5");
        System.out.println("tailMap 5->E: " + tailMap);

        //first
        System.out.println("firstKey: " + rocksMap.firstKey());

        //first
        System.out.println("lastKey:" + rocksMap.lastKey());

        rocksMap.put("name", "testdb");
        rocksMap.put("111", "testdb");
        rocksMap.put("333", "testdb");
        rocksMap.put("222", "testdb");
        System.out.println("getAll" + rocksMap.getAll(TObject.asList("111", "222", "333")));
        System.out.println("name:" + rocksMap.get("name"));
        rocksMap.removeAll(TObject.asList("111", "222"));
        System.out.println("KeySet: "+ rocksMap.keySet());
        System.out.println("search: " + JSON.toJSON(rocksMap.startWith("hh")));
        System.out.println("search: " + JSON.toJSON(rocksMap.startWith("hh",3, 30)));
        System.out.println("KeySet: "+ rocksMap.keySet());
        rocksMap.removeRange("hhhh5", "hhhh3");
        System.out.println("KeySet: "+ rocksMap.keySet());
    }

    public void testBackup() throws RocksDBException {
        //测试列族区分,这个列族不写入任何数据
        String cfName = "testdb1000";
        RocksMap rocksMap1 = new RocksMap(cfName);
        rocksMap1.put(System.currentTimeMillis(), System.currentTimeMillis());
        BackupableDBOptions backupableDBOptions = RocksMap.createBackupableOption();
        backupableDBOptions.setDestroyOldData(true);
        System.out.println(rocksMap1.createBackup());
    }


    public void testBackupInfo() throws RocksDBException {
        //测试列族区分,这个列族不写入任何数据
        String cfName = "testdb1000";
        RocksMap rocksMap1 = new RocksMap(cfName);
        List mm = rocksMap1.getBackupInfo();
        System.out.println(mm);
    }

    public void testRestoreLastBackup() throws RocksDBException {
        //测试列族区分,这个列族不写入任何数据
        String cfName = "testdb1000";
        RocksMap.restoreFromLatestBackup();
    }
}
