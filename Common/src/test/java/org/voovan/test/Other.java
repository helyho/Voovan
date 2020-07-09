package org.voovan.test;

import com.sun.jndi.toolkit.url.UrlUtil;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.WriteOptions;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.*;
import org.voovan.tools.collection.CacheMap;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;
import org.voovan.tools.serialize.TSerialize;
import sun.net.util.URLUtil;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

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
        TestObject t = new TestObject();
        t.setBint(1);
        t.setString("2");
        Vector l = new Vector<>();
        l.add(1111);
        t.setList(l);

        TestObject t1 = TObject.clone(t);
        l = new Vector<>();
        l.add(1122);
        t1.setList(l);
        t1.setBint(2);

        System.out.println(JSON.toJSONWithFormat(TObject.compare(t, t1, false)));
    }
}
