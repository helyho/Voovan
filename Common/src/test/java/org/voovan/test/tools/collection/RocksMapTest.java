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
        RocksMap rocksMap = new RocksMap(null);
        rocksMap.clear();
        System.out.println(rocksMap.isEmpty());
        System.out.println(rocksMap.size());

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

        //tailmap
        Map tailMap = rocksMap.tailMap("hhhh5");
        System.out.println("tailMap: " + tailMap);

        //headmap
        Map headMap = rocksMap.headMap("hhhh3");
        System.out.println("headMap: " + headMap);

        //first
        System.out.println("firstKey: " + rocksMap.firstKey());

        //first
        System.out.println("lastKey:" + rocksMap.lastKey());
    }
}
