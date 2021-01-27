package org.voovan.test;

import com.jsoniter.output.JsonStream;
import com.jsoniter.output.JsonStreamPool;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONEncode;
import org.voovan.tools.reflect.TReflect;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
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
    public static class Message {

        private String message;

        public Message(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public int hashCode() {
            return  -9;
        }
    }

    @Benchmark
    public static void withHash() throws Exception {
        JSONEncode.JSON_HASH =true;
        JSON.toJSON((new Message("hello word")));
    }

    @Benchmark
    public void withoutHash() throws Exception {
        JSONEncode.JSON_HASH =false;
        JSON.toJSON((new Message("hello word")));
    }

    @Benchmark
    public void withMethod() {
        Message message = new Message("hello word");
        json(message);
    }

    public String json(Message message) {
        return "{\""+"message" + "\":\"" + message.getMessage() + "\"}";
    }

    public static void main(String[] args) throws Exception {
        TReflect.register(Message.class);

        Options options = new OptionsBuilder()
                .include(JMH.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
