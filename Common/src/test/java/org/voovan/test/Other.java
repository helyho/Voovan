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
        byte c = (byte) -128;
        System.out.println((int)(c& 0xff));
        System.out.println((byte)(c+1)& 0xff);
        System.out.println((byte)(c+2)& 0xff);
        System.out.println((byte)(c+3)& 0xff);
        System.out.println((byte)(c+4)& 0xff);

//        128 = -128;  128 + (128 - 128)
//        129 = -127;  128 + (128 - 127);
//        130 = -126;  128 + (128 - 126)

    }
}
