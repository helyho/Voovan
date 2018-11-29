package org.voovan.tools.cache;

import java.util.Map;
import java.util.function.Function;

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


    /**
     * 为 Key 获取 key 的超时时间
     * @param key  key 名称
     * @return 超时时间
     */
    public long expire(K key);

    /**
     * 更新某个对象的超时时间
     *      可以为某个没有配置超时时间的键值对配置超时时间
     * @param key 键
     * @param expire 超时时间
     */
    public boolean expire(K key, long expire);


    /**
     * 获取对象
     *      不会刷新对象的时间戳
     * @param key 键
     * @param appointedSupplier 指定的获取器
     * @param createExpire 超时时间
     * @param refresh 刷新超时时间
     * @return 值
     */
    public V get(Object key, Function<K, V> appointedSupplier, Long createExpire, boolean refresh);


    default V get(Object key, Function<K, V> appointedSupplier, Long expire) {
        return get(key, appointedSupplier, expire, false);
    }

    /**
     * 获取对象
     *      不会刷新对象的时间戳
     * @param key 键
     * @param appointedSupplier 指定的获取器
     * @return 值
     */
    default V get(Object key, Function<K, V> appointedSupplier){
        return get(key, appointedSupplier, null, false);
    }

    /**
     * 获取对象
     *      不会刷新对象的时间戳
     * @param key 键
     * @return 值
     */
    default V get(Object key){
        return get(key, null, null, false);
    }

    /**
     * 获取并刷新对象
     * @param key 键
     * @param appointedSupplier 指定数据构造器
     * @param expire 超时时间
     * @return 值
     */
    default V getAndRefresh(Object key, Function<K, V> appointedSupplier, Long expire){
        return get(key, appointedSupplier, expire, true);
    }

    /**
     * 获取并刷新对象
     * @param key 键
     * @param appointedSupplier 指定数据构造器
     * @return 值
     */
    default V getAndRefresh(Object key, Function<K, V> appointedSupplier){
        return get(key, appointedSupplier, null, true);
    }

    /**
     * 获取并刷新对象
     * @param key 键
     * @return 值
     */
    default V getAndRefresh(Object key){
        return get(key, null, null, true);
    }
}

