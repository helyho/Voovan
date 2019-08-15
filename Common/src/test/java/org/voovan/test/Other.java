package org.voovan.test;

import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.WriteOptions;
import org.voovan.tools.TByte;
import org.voovan.tools.TDateTime;
import org.voovan.tools.UniqueId;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.serialize.TSerialize;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Other {
    private static int apple = 10;
    private int orange = 10;

    public static void main(String[] args) throws Exception {
        DBOptions dbOptions = new DBOptions();
        ReadOptions readOptions = new ReadOptions();
        WriteOptions writeOptions = new WriteOptions();
        ColumnFamilyOptions columnFamilyOptions = new ColumnFamilyOptions();

        RocksMap rocksMap =  new RocksMap("waltest", "testdb", columnFamilyOptions, dbOptions, readOptions, writeOptions, true);
        System.out.println(rocksMap.get(11));

    }
}
