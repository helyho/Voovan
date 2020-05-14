package org.voovan.test;

import com.sun.jndi.toolkit.url.UrlUtil;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.WriteOptions;
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
        TreeMap<String, Object> m = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);

        m.put("cookie", 1);

        System.out.println(m.get("Cookie"));
    }
}
