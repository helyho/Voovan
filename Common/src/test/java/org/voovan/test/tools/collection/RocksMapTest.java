package org.voovan.test.tools.collection;

import org.rocksdb.RocksDBException;
import org.voovan.tools.collection.RocksMap;

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

        RocksMap rocksMap = new RocksMap("one", "testdb");
//        RocksMap rocksMap = new RocksMap("one", "testdb", true);
        System.out.println(rocksMap.get("name"));

        rocksMap.clear();
        System.out.println("isEmpty: " + rocksMap.isEmpty());
        System.out.println("size: " + rocksMap.size());

        //rollback
        rocksMap.beginTransaction();
        rocksMap.put("transaction", "rocksMap.value");
        rocksMap.rollback();
        System.out.println("rollback get: "+ rocksMap.get("transaction"));

        //commit
        rocksMap.beginTransaction();
        rocksMap.put("transaction", "rocksMap.value");
        rocksMap.commit();
        System.out.println("commit get: "+ rocksMap.get("transaction"));

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
    }
}
