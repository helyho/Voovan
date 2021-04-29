package org.voovan.tools.buffer;

import org.voovan.Global;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/**
 * ByteBuffer 内存分配分析类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ByteBufferAnalysis {
    public final static LongAdder MALLOC_SIZE       = new LongAdder();
    public final static LongAdder MALLOC_COUNT      = new LongAdder();
    public final static LongAdder BYTE_BUFFER_COUNT = new LongAdder();

    public final static int BYTE_BUFFER_ANALYSIS  = TEnv.getSystemProperty("ByteBufferAnalysis", 0);

    public static void malloc(int capacity) {
        if(BYTE_BUFFER_ANALYSIS >= 0) {
            MALLOC_SIZE.add(capacity);
            MALLOC_COUNT.increment();
            BYTE_BUFFER_COUNT.increment();
        }
    }

    public static void realloc(int oldCapacity, int newCapacity) {
        if(BYTE_BUFFER_ANALYSIS >= 0) {
            MALLOC_SIZE.add(newCapacity - oldCapacity);
        }
    }


    public static void free(int capacity) {
        if(BYTE_BUFFER_ANALYSIS >= 0) {
            MALLOC_SIZE.add(-1 * capacity);
            MALLOC_COUNT.decrement();
            BYTE_BUFFER_COUNT.decrement();
        }
    }

    public static Map<String, Long> getByteBufferAnalysis() {
        return TObject.asMap("Time", TDateTime.now(), "MallocSize", TString.formatBytes(MALLOC_SIZE.longValue()),
                "MallocCount", MALLOC_COUNT.longValue(),
                "ByteBufferCount", BYTE_BUFFER_COUNT.longValue());
    }

    static {
        if(BYTE_BUFFER_ANALYSIS > 0) {
            Global.getHashWheelTimer().addTask(() -> {
                Logger.simple(getByteBufferAnalysis());
            }, BYTE_BUFFER_ANALYSIS);
        }
    }
}
