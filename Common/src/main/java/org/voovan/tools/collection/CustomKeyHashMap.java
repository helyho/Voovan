package org.voovan.tools.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 自定 key 转换的 hashMap
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class CustomKeyHashMap<K, V> extends HashMap {
    public Function<K, K> hash;

    public CustomKeyHashMap(int initialCapacity, float loadFactor, Function<K, K> hash) {
        super(initialCapacity, loadFactor);
        this.hash = hash;
    }

    public CustomKeyHashMap(int initialCapacity, Function<K, K> hash) {
        super(initialCapacity);
        this.hash = hash;
    }

    public CustomKeyHashMap(Function<K, K> hash) {
        this.hash = hash;
    }

    public CustomKeyHashMap(Map<K, V> m, Function<K, K> hash) {
        super(m);
        this.hash = hash;
    }

    @Override
    public Object get(Object key) {
        return super.get(hash.apply((K)key));
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(hash.apply((K)key));
    }

    @Override
    public Object put(Object key, Object value) {
        return super.put(hash.apply((K)key), value);
    }

    @Override
    public void putAll(Map m) {
        for(Object obj : m.entrySet()) {
            Map.Entry entry = (Map.Entry) obj;
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object remove(Object key) {
        return super.remove(hash.apply((K)key));
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return super.getOrDefault(hash.apply((K)key), defaultValue);
    }

    @Override
    public Object putIfAbsent(Object key, Object value) {
        return super.putIfAbsent(hash.apply((K)key), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return super.remove(hash.apply((K)key), value);
    }

    @Override
    public boolean replace(Object key, Object oldValue, Object newValue) {
        return super.replace(hash.apply((K)key), oldValue, newValue);
    }

    @Override
    public Object replace(Object key, Object value) {
        return super.replace(hash.apply((K)key), value);
    }

    @Override
    public Object computeIfAbsent(Object key, Function mappingFunction) {
        return super.computeIfAbsent(hash.apply((K)key), mappingFunction);
    }

    @Override
    public Object computeIfPresent(Object key, BiFunction remappingFunction) {
        return super.computeIfPresent(hash.apply((K)key), remappingFunction);
    }

    @Override
    public Object compute(Object key, BiFunction remappingFunction) {
        return super.compute(hash.apply((K)key), remappingFunction);
    }

    @Override
    public Object merge(Object key, Object value, BiFunction remappingFunction) {
        return super.merge(hash.apply((K)key), value, remappingFunction);
    }
}
