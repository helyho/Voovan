package org.voovan.test;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.security.THash;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Other {

    public static void main(String[] args) throws IOException {
        ConcurrentSkipListMap kv = new ConcurrentSkipListMap();
        String mmm = "wrk -H 'Host: tfb-server' -H 'Accept: text/plain,text/html;q=0.9,application/xhtml+xml;q=0.9,application/xml;q=0.8,*/*;q=0.7' -H 'Connection: keep-alive' --latency -d 15 -c 1024 --timeout 8 -t 20 http://tfb-server:8080/plaintext -s pipeline.lua -- 16";
        String mmm1 = "wrk -H 'Host: tfb-server' -H 'Accept: text/plain,text/html;q=0.9,application/xhtml+xml;q=0.9,application/xml;q=0.8,*/*;q=0.7' -H 'Connection: keep-alive' --latency -d 15 -c 1024 --timeout 8 -t 20 http://tfb-server:8080/plaintext -s pipeline.lua -- 16";

        System.out.println(TEnv.measureTime(()->{
            int code = 0;
            for(int i = 0;i <1000000;i++){
                StringBuilder m = new StringBuilder(mmm+i).append(mmm1);
                String mv = m.toString();
                kv.put(mv, 000);
                for(int p=0;p<10;p++) {
                    kv.get(mv);
                }
//                code = m.toString().hashCode();
            }
            return code;
        }));

        ConcurrentSkipListMap kv1 = new ConcurrentSkipListMap();

        System.out.println(TEnv.measureTime(()->{
            int code = 0;
            for(int i = 0;i <1000000;i++){
                code = THash.hashTime31(mmm+i, mmm1);
                kv1.put(code, 000);
                for(int p=0;p<10;p++) {
                    kv1.get(code);
                }
            }
            return code;
        }));

        ConcurrentSkipListMap kv2 = new ConcurrentSkipListMap();

        System.out.println(TEnv.measureTime(()->{
            String code = "";
            for(int i = 0;i <1000000;i++){
                code = THash.encryptMD5(mmm+i + mmm1);
                kv2.put(code, 000);
                for(int p=0;p<10;p++) {
                    kv2.get(code);
                }
            }
            return code;
        }));
    }
}
