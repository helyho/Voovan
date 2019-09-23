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
        String url = "http://192.168.123.150:8848/nacos/v1/cs/configs?dataId=test.config.propertiest&group=DEFAULT_GROUP";
        System.out.println(TProperties.getString(url, "common.rdbPath"));
        System.out.println(TProperties.getString(url, "common.siteId"));
        System.out.println(TProperties.getString("framework", "EnableSandBox"));
        System.out.println(TProperties.getString("framework", "ThreadPoolMaxSize"));

    }
}
