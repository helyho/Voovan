package org.voovan.test;

import com.sun.jndi.toolkit.url.UrlUtil;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.WriteOptions;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.*;
import org.voovan.tools.collection.CacheMap;
import org.voovan.tools.collection.Chain;
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
        Chain<String[]> m = new Chain<>();

        m.add(new String[]{"11", "22", "33"});
        TEnv.measure("clone", ()->{
            for (int i=0;i<1000000;i++) {
                m.clone();
                String ml = m.getContianer().get(0)[0];
            }
        });

        final Object[] x = new Object[]{m};
        TEnv.measure("cache", ()->{
            for (int i=0;i<1000000;i++) {
               String ml =  ((Chain<String[]>)x[0]).getContianer().get(0)[0];
            }
        });
    }
}
