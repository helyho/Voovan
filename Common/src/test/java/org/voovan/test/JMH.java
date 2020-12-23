package org.voovan.test;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.security.THash;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
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
    static String m = new String("abcdabcdefghijkabcdefghijkabcdefghijkabcdefghijkabcdefghijkabcdefghijkabcdefghijkefghijk");
    static TestObject testObject = new TestObject();
    static Vector ml = new Vector();

    static {
        testObject.setBint(11);
    }

    @Benchmark
    public void oldRef() throws ReflectiveOperationException {
        for(int i=0;i<10000;i++) {
            m.hashCode();
            TReflect.setFieldValue(testObject, "bint", 111);
//            TReflect.getFields(TestObject.class);
        }
    }

    @Benchmark
    public void newRef() {
        for(int i=0;i<10000;i++) {
            testObject.setBint(111);
//            TReflect2.findField(TestObject.class, "TestObject2Arr");
//            TReflect2.getFields(TestObject.class);
        }
    }

    public static void main(String[] args) throws Exception {
        TReflect.register(TestObject.class);

        TReflect.invokeMethod(testObject, "setList", ml);
        Options options = new OptionsBuilder()
                .include(JMH.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
