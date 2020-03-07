package org.voovan.test;

import com.sun.jndi.toolkit.url.UrlUtil;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.WriteOptions;
import org.voovan.tools.*;
import org.voovan.tools.collection.RocksMap;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.serialize.TSerialize;
import sun.net.util.URLUtil;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Properties;

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
        byte[] bs1 = new byte[] {0, 40, 0, -97, };
        byte[] bs2 = new byte[] {0, 0, -128, -48};
        System.out.println(TByte.getInt(bs1));
        System.out.println(TByte.getInt(bs2));
        System.out.println(compare(bs1, bs2));


    }

    private static int compare(final byte[] a, final byte[] b) {
        return ByteBuffer.wrap(a).compareTo(ByteBuffer.wrap(b));
    }
}
