package org.voovan.tools.collection;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * long 作为 key 的 map
 * 参考开放地址方法
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */

public class LongKeyMap<T> {
    private static final int FREE_KEY = 0;

    public final T FREE_VALUE = null;

    private long[] keys;

    private T[] values;

    private final float fillFactor;

    private int capacity;

    private int threshold;

    private int size;

    private int mask;

    public LongKeyMap(final int size) {
        this(size, 0.75f);
    }

    public LongKeyMap(final int size, final float fillFactor) {
        if (fillFactor <= 0 || fillFactor >= 1)
            throw new IllegalArgumentException("FillFactor must be in (0, 1)");
        if (size <= 0)
            throw new IllegalArgumentException("Size must be positive!");
        final int capacity = getPowerOfTwoSize(size, fillFactor);
        mask = capacity - 1;
        this.fillFactor = fillFactor;

        this.capacity = capacity;
        keys = new long[capacity];
        values = null;
        values = (T[]) new Object[capacity];
        threshold = (int) (capacity * fillFactor);
    }

    public int getCapacity() {
        return capacity;
    }

    public T get(final long key) {
        if (key == FREE_KEY)
            return FREE_VALUE;

        final int index = getReadIndex(key);
        return index != -1 ? values[index] : FREE_VALUE;
    }

    public T put(final long key, final T value) {
        if (key == FREE_KEY) {
            final T ret = FREE_VALUE;
            return ret;
        }

        int index = getPutIndex(key);
        if (index < 0) {
            rehash((int) (keys.length * 3));
            index = getPutIndex(key);
        }
        final T prev = values[index];
        if (keys[index] != key) {
            keys[index] = key;
            values[index] = value;
            ++size;
            if (size >= threshold)
                rehash(keys.length * 2);
        } else {
            assert keys[index] == key;
            values[index] = value;
        }
        return prev;
    }

    public T putIfAbsent(long key, T value) {
        T t = get(key);
        if (t == null) {
            t = put(key, value);
        }
        return t;
    }

    public T remove(final long key) {
        if (key == FREE_KEY) {
            return FREE_VALUE;
        }

        int index = getReadIndex(key);
        if (index == -1)
            return FREE_VALUE;

        final T res = values[index];
        values[index] = FREE_VALUE;
        --size;
        return res;
    }

    public void clear() {
        Arrays.fill(keys, 0);
        Arrays.fill(values, null);
        size = 0;
    }

    public int size() {
        return size;
    }

    public long getKey(int index) {
        return keys[index];
    }

    public T getValue(int index) {
        return values[index];
    }

    private void rehash(final int newCapacity) {
        threshold = (int) (newCapacity * fillFactor);
        mask = newCapacity - 1;

        final int oldCapacity = keys.length;
        final long[] oldKeys = keys;
        final T[] oldValues = values;

        this.capacity = capacity;
        keys = new long[newCapacity];
        values = (T[]) new Object[newCapacity];

        for (int i = oldCapacity; i-- > 0; ) {
            if (oldKeys[i] != FREE_KEY)
                put(oldKeys[i], oldValues[i]);
        }
    }

    private int getReadIndex(final long key) {
        int index = getStartIndex(key);
        //地址已被使用
        if (keys[index] == key) {
            return index;
        }

        //地址未被使用
        if (keys[index] == FREE_KEY) {
            return -1;
        }

        //地址已被使用且 key 和期望的不同,则 index + 1 & mask, 获得下一个地址
        final int startIdx = index;
        while ((index = getNextIndex(index)) != startIdx) {
            if (keys[index] == FREE_KEY)
                return -1;
            if (keys[index] == key)
                return index;
        }
        return -1;
    }

    private int getPutIndex(final long key) {
        //地址已被使用
        final int readIdx = getReadIndex(key);
        if (readIdx >= 0) {
            return readIdx;
        }

        //地址未被使用
        final int startIdx = getStartIndex(key);
        if (keys[startIdx] == FREE_KEY) {
            return startIdx;
        }

        //地址已被使用且 key 和期望的不同,则 index + 1 & mask, 获得下一个地址
        int nextIdx = startIdx;
        while (keys[nextIdx] != FREE_KEY) {
            nextIdx = getNextIndex(nextIdx);
            if (nextIdx == startIdx)
                return -1;
        }
        return nextIdx;
    }


    private int getStartIndex(final long key) {
        return phiMix(key) & mask;
    }

    private int getNextIndex(final int currentIndex) {
        return (currentIndex + 1) & mask;
    }


    private static long nextPowerOfTwo(long x) {
        if (x == 0) return 1;
        x--;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return (x | x >> 32) + 1;
    }

    private static int getPowerOfTwoSize(final int expected, final float f) {
        final long s = Math.max(2, nextPowerOfTwo((long) Math.ceil(expected / f)));
        if (s > (1 << 30))
            throw new IllegalArgumentException("Too large (" + expected + " expected elements with load factor " + f + ")");
        return (int) s;
    }

    private static final int INT_PHI = 0x9E3779B9;

    private static int phiMix(long value) {
        int h = Long.hashCode(value) * INT_PHI;
        return h ^ (h >> 16);
    }

}
