package org.hocate.util;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * 一对多的 map .
 * 		实际存储为 key,List<V>的形式
 * 
 * @author helyho
 *
 * @param <K>
 * @param <V>
 */
public class MulitMap<K, V> implements Map<K, V> {

	public Map<K, List<V>>	superMap;

	public MulitMap() {
		superMap = new Hashtable<K, List<V>>();
	}

	@Override
	public int size() {
		superMap.size();
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return superMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return superMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (Entry<K, List<V>> entry : superMap.entrySet()) {
			if (entry.getValue().contains(value)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V get(Object key) {
		List<V> valueList = superMap.get(key);
		// 返回 List 中最后一个元素
		return valueList == null ? null : valueList.get(valueList.size() - 1);
	}
	
	/**
	 * 获取 key 对应的 value
	 * @param key  
	 * @return	
	 */
	public List<V> mulitGet(Object key) {
		return superMap.get(key);
	}

	@Override
	@Deprecated
	public V put(K key, V value) {
		List<V> valueList = superMap.get(key);
		if (valueList == null) {
			valueList = new Vector<V>();
			superMap.put(key, valueList);
		}
		valueList.add(value);
		return value;
	}

	/**
	 *  填充 key,value 到 map
	 * @param key
	 * @param value
	 * @return
	 */
	public List<V> mulitPut(K key, V value) {
		List<V> valueList = superMap.get(key);
		if (valueList == null) {
			valueList = new Vector<V>();
			superMap.put(key, valueList);
		}
		valueList.add(value);
		return valueList;
	}

	@Override
	@Deprecated
	public V remove(Object key) {
		List<V> valueList = superMap.get(key);
		superMap.remove(key);

		// 返回 List 中最后一个元素
		if (valueList != null) {
			return valueList.get(valueList.size() - 1);
		}
		return null;
	}

	/**
	 * 移除某个 key
	 * @param key
	 * @return
	 */
	public List<V> mulitRemove(Object key) {
		List<V> valueList = superMap.get(key);
		superMap.remove(key);
		return valueList;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
			K key = entry.getKey();
			V value = entry.getValue();
			List<V> valueList = null;
			if (superMap.containsKey(key)) {
				valueList = superMap.get(key);
			} else {
				valueList = new Vector<V>();
				superMap.put(key, valueList);
			}

			if (!valueList.contains(value)) {
				valueList.add(value);
			}
		}
	}
	
	/**
	 * 填充一个 map 到当前对象
	 * @param m
	 */
	public void PutAll(Map<? extends K, ? extends List<V>> m) {
		for (Entry<? extends K, ? extends List<V>> entry : m.entrySet()) {
			K key = entry.getKey();
			List<V> value = entry.getValue();
			List<V> valueList = null;
			if (superMap.containsKey(key)) {
				valueList = superMap.get(key);
			} else {
				valueList = new Vector<V>();
				superMap.put(key, valueList);
			}

			if (!valueList.contains(value)) {
				valueList.addAll(m.get(key));
			}
		}
	}

	@Override
	public void clear() {
		superMap.clear();
	}

	@Override
	public Set<K> keySet() {
		return superMap.keySet();
	}

	@Override
	@Deprecated
	public Collection<V> values() {
		Collection<V> values = new Vector<V>();
		for (List<V> value : superMap.values()) {
			values.add(value.get(value.size() - 1));
		}
		return values;
	}

	/**
	 * 返回所有key 的value集合
	 * @return
	 */
	public Collection<List<V>> mulitValues() {
		return superMap.values();
	}

	@Override
	@Deprecated
	public Set<Entry<K, V>> entrySet() {
		HashSet<Entry<K, V>> entrySet = new HashSet<Entry<K, V>>();
		for (Entry<K, List<V>> entry : superMap.entrySet()) {
			List<V> valueList = entry.getValue();
			SimpleEntry<K, V> simpleEntry = new SimpleEntry<K, V>(entry.getKey(), valueList.get(valueList.size() - 1));
			entrySet.add(simpleEntry);
		}
		return entrySet;
	}

	/**
	 * 返回 map 的 Entry 集合
	 * @return
	 */
	public Set<Entry<K, List<V>>> mulitEntrySet() {
		return superMap.entrySet();
	}
}
