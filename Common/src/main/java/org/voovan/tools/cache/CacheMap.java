package org.voovan.tools.cache;

import java.util.Map;

/**
 * 类文字命名
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public interface CacheMap<K, V> extends Map<K,V>{

    public V put(K key, V value, int expire);

    public V putIfAbsent(K key, V value, int expire);

    public void putAll(Map<? extends K, ? extends V> map, int expire);
}
