package org.voovan.tools;

/**
 * 多值 Map
 *
 * @author helyho
 *         <p>
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */

import org.voovan.tools.json.JSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class MultiMap<K,V>
        extends HashMap<K, List<V>>
{
    public MultiMap() {}

    public MultiMap(Map<K, List<V>> map)
    {
        super(map);
    }

    /**
     * 获取值的集合
     * @param key 键
     * @return 值的集合
     */
    public List<V> getValues(K key)
    {
        List<V> vals = (List)super.get(key);
        if ((vals == null) || (vals.isEmpty())) {
            return null;
        }
        return vals;
    }

    /**
     * 获取值
     * @param key 键
     * @param i 值的索引
     * @return 值
     */
    public V getValue(K key, int i)
    {
        List<V> vals = getValues(key);
        if (vals == null) {
            return null;
        }
        if ((i == 0) && (vals.isEmpty())) {
            return null;
        }
        return (V)vals.get(i);
    }

    /**
     * 增加值
     * @param key   键
     * @param value  值
     * @return  值的集合
     */
    public List<V> putValue(K key, V value)
    {
        if (value == null) {
            return (List)super.put(key, null);
        }
        List<V> vals = new ArrayList();
        vals.add(value);
        return (List)super.put((K)key, vals);
    }

    /**
     * 一次插入多个键值
     * @param input 键/值(List)的 Map 对象
     */
    public void putAllValues(Map<K, V> input)
    {
        for (Map.Entry<K, V> entry : input.entrySet()) {
            putValue((K)entry.getKey(), entry.getValue());
        }
    }

    /**
     * 一次插入一个键值
     * @param key 键
     * @param values 值的集合
     * @return
     */
    public List<V> putValues(K key, List<V> values)
    {
        return (List)super.put(key, values);
    }

    /**
     * 一次插入一个键值
     * @param key 键
     * @param values 值的数组
     * @return
     */
    @SafeVarargs
    public final List<V> putValues(K key, V... values)
    {
        List<V> list = new ArrayList();
        list.addAll(Arrays.asList(values));
        return (List)super.put(key, list);
    }

    /**
     * 一次插入一个键值
     * @param key 键
     * @param value 值
     * @return
     */
    public void add(K key, V value)
    {
        List<V> lo = (List)get(key);
        if (lo == null) {
            lo = new ArrayList();
        }
        lo.add(value);
        super.put(key, lo);
    }

    /**
     * 增加键的一系列的值
     * @param key 键
     * @param values 值的集合
     */
    public void addValues(K key, List<V> values)
    {
        List<V> lo = (List)get(key);
        if (lo == null) {
            lo = new ArrayList();
        }
        lo.addAll(values);
        super.put(key, lo);
    }

    /**
     * 增加键的一系列的值
     * @param key 键
     * @param values 值的集合
     */
    public void addValues(K key, V[] values)
    {
        List<V> lo = (List)get(key);
        if (lo == null) {
            lo = new ArrayList();
        }
        lo.addAll(Arrays.asList(values));
        super.put(key, lo);
    }

    /**
     * 增加键/值
     * @param map 键/值(List)的 Map 对象
     */
    public boolean addAllValues(MultiMap<K,V> map)
    {
        boolean merged = false;
        if ((map == null) || (map.isEmpty())) {
            return merged;
        }
        for (Map.Entry<K, List<V>> entry : map.entrySet())
        {
            K name = (K)entry.getKey();
            List<V> values = (List)entry.getValue();
            if (containsKey(name)) {
                merged = true;
            }
            addValues(name, values);
        }
        return merged;
    }

    /**
     * 移除某个值
     * @param key  键
     * @param value 值
     * @return  成功:true , 失败:false
     */
    public boolean removeValue(K key, V value)
    {
        List<V> lo = (List)get(key);
        if ((lo == null) || (lo.isEmpty())) {
            return false;
        }
        boolean ret = lo.remove(value);
        if (lo.isEmpty()) {
            remove(key);
        } else {
            super.put(key, lo);
        }
        return ret;
    }

    /**
     * 是否包含某个值
     * @param value 值
     * @return 成功:true , 失败:false
     */
    public boolean containsValues(V value)
    {
        for (List<V> vals : values()) {
            if ((vals.size() == 1) && (vals.contains(value))) {
                return true;
            }
        }
        return false;
    }

    public String toString()
    {
        return JSON.toJSON(this);
    }

}