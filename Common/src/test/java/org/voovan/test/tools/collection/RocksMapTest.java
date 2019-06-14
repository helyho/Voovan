package org.voovan.test.tools.collection;

import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.json.JSON;

import java.util.HashMap;
import java.util.Map;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class RocksMapTest {

    public static void main(String[] args) throws RocksDBException {
        //测试列族区分,这个列族不写入任何数据
        String cfName = "testdb1000";
        RocksMap rocksMap1 = new RocksMap(cfName);
        System.out.println(rocksMap1.get("name"));
        if(rocksMap1.get("name") == null){
            System.out.println("putIfAbsent: "+ rocksMap1.putIfAbsent("name", cfName));
            System.out.println("putIfAbsent: "+ rocksMap1.putIfAbsent("name", cfName));

            System.out.println("replace: "+ rocksMap1.replace("name1", cfName));
            System.out.println("replace: "+ rocksMap1.replace("name1", cfName));
        }

        RocksMap rocksMap = new RocksMap("testdb");
        System.out.println(rocksMap.get("name"));

        rocksMap.clear();
        rocksMap1.clear();

        System.out.println("isEmpty: " + rocksMap.isEmpty());
        System.out.println("size: " + rocksMap.size());

        //rollback
        System.out.println("===============rollback==================");
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
        rocksMap.choseColumnFamily("testdb");

        //commit
        System.out.println("===============commit==================");
        rocksMap.beginTransaction();
        rocksMap.put("transaction11", "rocksMap.value");
        rocksMap.choseColumnFamily(cfName);
        rocksMap.put("transaction11", cfName);
        rocksMap.commit();

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap  get: "+ rocksMap.get("transaction11"));
        rocksMap.share(cfName);
        System.out.println("rocksMap1 get: "+ rocksMap1.get("transaction11"));
        rocksMap.choseColumnFamily("testdb");

        System.out.println("===============withTransaction commit==================");
        rocksMap.withTransaction(map ->{
            RocksMap rocksMapT = (RocksMap)map;
            rocksMapT.put("ddddk", "ffdasf");
            System.out.println("withTransaction commit: " + rocksMapT.get("ddddk"));
            return true;
        });

        System.out.println("withTransaction commit: " + rocksMap.get("ddddk"));
        rocksMap.remove("ddddk");

        //rollback
        System.out.println("===============withTransaction rollback==================");
        rocksMap.withTransaction(map ->{
            RocksMap rocksMapT = (RocksMap)map;
            rocksMapT.put("ddddk", "ffdasf");
            System.out.println("withTransaction rollback: " + rocksMapT.get("ddddk"));
            return false;
        });
        System.out.println("withTransaction rollback: " + rocksMap.get("ddddk"));


        //rollback
        System.out.println("===============share rollback==================");
        rocksMap.beginTransaction();
        rocksMap.put("transaction22", "rocksMap.value");
        RocksMap rocksMap2 = rocksMap.share(cfName);
        rocksMap2.put("transaction22", cfName);

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap share get: "+ rocksMap.get("transaction22"));
        System.out.println("rocksMap2 share get: "+ rocksMap2.get("transaction22"));
        rocksMap.rollback();

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap share rollback get: "+ rocksMap.get("transaction22"));
        System.out.println("rocksMap2 share rollback get: "+ rocksMap2.get("transaction22"));
        rocksMap.choseColumnFamily("testdb");

        //commit
        System.out.println("===============share commit==================");
        rocksMap.beginTransaction();
        rocksMap.put("transaction33", "rocksMap.value");
        rocksMap2 = rocksMap.share(cfName);
        rocksMap2.put("transaction33", cfName);
        rocksMap.commit();

        rocksMap.choseColumnFamily("testdb");
        System.out.println("rocksMap share get: "+ rocksMap.get("transaction33"));
        System.out.println("rocksMap2 share get: "+ rocksMap2.get("transaction33"));
        rocksMap.choseColumnFamily("testdb");
        System.out.println("=======================================");


        //put
        rocksMap.put("aaaa", "bbbb");
        rocksMap.put("cccc", "dddd");
        rocksMap.put("eeee", "ffff");
        rocksMap.put("hhhh", "iiii");


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
            System.out.println(iterator.key() + " " + iterator.value());
            iterator.next();
        }
        System.out.println("=================iterator first================");
        iterator = rocksMap.iterator();
        while(iterator.hasNext()){
            System.out.println(iterator.key() + " " + iterator.value());
            iterator.next();
        }
        System.out.println("=================iterator last================");
        iterator = rocksMap.iterator();
        while(iterator.hasNext()){
            System.out.println(iterator.key() + " " + iterator.value());
            iterator.next();
        }

        System.out.println("=================iterator remove================");
        iterator = rocksMap.iterator();
        while(iterator.hasNext()){
            iterator.remove();
            break;
        }
        iterator = rocksMap.iterator();
        while(iterator.hasNext()){
            System.out.println(iterator.key() + " " + iterator.value());
            iterator.next();
        }
        System.out.println("=================iterator range================");
        iterator = rocksMap.iterator("eeee", "transaction11");
        while(iterator.hasNext()) {
            System.out.println(iterator.key() + " " + iterator.value());
            iterator.next();
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
        rocksMap.putAll(data);
        System.out.println("size: "+ rocksMap.size());
        System.out.println("KeySet: "+ rocksMap.keySet());

        //submap
        Map subMap = rocksMap.subMap("hhhh2", "hhhh5");
        System.out.println("subMap: " + subMap);


        //headmap
        Map headMap = rocksMap.headMap("hhhh4");
        System.out.println("headMap: " + headMap);

        //tailmap
        Map tailMap = rocksMap.tailMap("hhhh5");
        System.out.println("tailMap: " + tailMap);

        //first
        System.out.println("firstKey: " + rocksMap.firstKey());

        //first
        System.out.println("lastKey:" + rocksMap.lastKey());

        rocksMap.put("name", "testdb");
        System.out.println("name:" + rocksMap.get("name"));
        System.out.println("KeySet: "+ rocksMap.keySet());
        System.out.println("search: " + JSON.toJSON(rocksMap.startWith("na")));

    }
}
