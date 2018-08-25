package org.voovan.tools.cache;

import java.util.Map;

/**
 * 缓存的基础类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public interface CacheMap<K, V> extends Map<K,V>{

    public V put(K key, V value, long expire);

    public V putIfAbsent(K key, V value, long expire);

    public void putAll(Map<? extends K, ? extends V> map, long expire);
}
