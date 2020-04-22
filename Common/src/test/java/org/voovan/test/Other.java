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
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
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
        long x = 7340076564580663875l;

        System.out.println(x << 32 >>> 32);
        System.out.println(x & 0x00000000FFFFFFFFL);
        TEnv.measure(">>", ()->{
            for(int i=0;i<100000000;i++) {
                long y = x << 32 >>> 32;
            }
        });

        TEnv.measure(">>", ()->{
            for(int i=0;i<100000000;i++) {
                long y = x & 0x00000000FFFFFFFFL;
            }
        });

        TEnv.measure(">>", ()->{
            for(int i=0;i<100000000;i++) {
                long y = x << 32 >>> 32;
            }
        });

        TEnv.measure(">>", ()->{
            for(int i=0;i<100000000;i++) {
                long y = x & 0x00000000FFFFFFFFL;
            }
        });
    }
}
