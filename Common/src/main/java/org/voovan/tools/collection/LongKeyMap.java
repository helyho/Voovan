package org.voovan.tools.collection;

import java.util.AbstractMap;
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
    private IntKeyMap<Integer> highKeyMap;
    private IntKeyMap<T> lowKeyMap;

    public LongKeyMap(final int size) {
        this(size, 0.75f);
    }

    public LongKeyMap(final int size, final float fillFactor) {
        highKeyMap = new IntKeyMap<>(size, fillFactor);
        lowKeyMap = new IntKeyMap<>(size, fillFactor);
    }

    public int getCapacity() {
        return highKeyMap.getCapacity();
    }

    public T get(final long key) {
       int hi = (int) (key >>> 32);
       int low = (int) (key << 32 >>> 32);

       Integer lowKey = highKeyMap.get(hi);

       if(lowKey == null) {
           return null;
       }

       return lowKeyMap.get(lowKey);
    }

    public T put(final long key, final T value) {
        int hi = (int) (key >>> 32);
        int low = (int) (key << 32 >>> 32);

        highKeyMap.put(hi, low);
        return lowKeyMap.put(low, value);
    }

    public T putIfAbsent(long key, T value) {
        T t = get(key);
        if (t == null) {
            t = put(key, value);
        }
        return t;
    }

    public T remove(final long key) {
        int hi = (int) (key >>> 32);
        int low = (int) (key << 32 >>> 32);

        highKeyMap.remove(hi);
        return lowKeyMap.remove(low);

    }

    public int size() {
        return highKeyMap.size();
    }

    public long getKey(int index) {
        int hiKey = highKeyMap.getKey(index);
        if(hiKey != 0) {
            int lowKey = highKeyMap.get(hiKey);
            return ((long) hiKey) << 32 | ((long) lowKey);
        } else {
            return 0l;
        }
    }

    public T getValue(int index) {
        return lowKeyMap.getValue(index);
    }

    public void clear() {
        highKeyMap.clear();
        lowKeyMap.clear();
    }

}
