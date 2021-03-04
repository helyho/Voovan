package org.voovan.test;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.UniqueId;
import org.voovan.tools.collection.LongKeyMap;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONEncode;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(3)
@Fork(1)
//@OutputTimeUnit(TimeUnit.)
public class JMH {
    public static UniqueId uniqueId = new UniqueId();
    public static ConcurrentHashMap<Long, Long> x = new ConcurrentHashMap<>();
    public static LongKeyMap<Long> y = new LongKeyMap<Long>(64);
    public static long id = uniqueId.nextNumber();
    static {
        x.put(id, id);

        y.put(id, id);
    }

    @Benchmark
    public static void withHash() throws Exception {
        for(int i=0;i<100000;i++) {
            x.get(id);
        }
    }


    @Benchmark
    public void withArray() {
        for(int i=0;i<100000;i++) {
            x.get(id);
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new OptionsBuilder()
                .include(JMH.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
