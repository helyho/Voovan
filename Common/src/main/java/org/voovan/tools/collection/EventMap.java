package org.voovan.tools.collection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * 基于事件的 Map 方便对 map 的修改进行跟踪
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class EventMap<K, V> implements Map {
    public enum EventType {
        CREATE,
        GET,
        PUT,
        PUT_ALL,
        REMOVE,
        CLEAR
    }

    public class EventItem<K,V> {
        private Map<K,V> parent;
        private K key;
        private V value;
        private Map data;

        public EventItem(Map<K,V> parent) {
            this.parent = parent;
        }

        public EventItem(Map<K,V> parent, K key) {
            this.parent = parent;
            this.key = key;
        }

        public EventItem(Map<K,V> parent, K key, V value) {
            this.parent = parent;
            this.key = key;
            this.value = value;
        }

        public EventItem(Map<K,V> parent, Map data) {
            this.parent = parent;
            this.data = data;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public Map getData() {
            return data;
        }

        @Override
        public String toString() {
            return "EventItem{" +
                    "key=" + key +
                    ", value=" + value +
                    ", data=" + data +
                    '}';
        }
    }

    private Map<K, V> conatiner;
    private BiFunction<EventType, EventItem, Boolean> event;

    public EventMap(Map<K, V> contianer,  BiFunction<EventType, EventItem, Boolean> event) {
        if(contianer == null) {
            throw new NullPointerException("the contianer param is null");
        }

        if(event == null) {
            throw new NullPointerException("the event param is null");
        }

        this.event = event;
        if(this.event.apply(EventType.CREATE, new EventItem<K, V>(this, contianer))) {
            this.conatiner = contianer;
        }
    }

    public BiFunction<EventType, EventItem, Boolean> getEvent() {
        return event;
    }

    public void setEvent(BiFunction<EventType, EventItem, Boolean> onEvent) {
        if(event == null) {
            throw new NullPointerException("the event param is null");
        }
        this.event = onEvent;
    }

    public Map<K, V> getConatiner() {
        return conatiner;
    }

    @Override
    public int size() {
        return conatiner.size();
    }

    @Override
    public boolean isEmpty() {
        return conatiner.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return conatiner.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return conatiner.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if(event.apply(EventType.GET, new EventItem<K, V>(this, (K)key))) {
            return conatiner.get(key);
        } else {
            return null;
        }

    }

    @Override
    public V put(Object key, Object value) {
        if(event.apply(EventType.PUT, new EventItem<K, V>(this, (K)key, (V)value))) {
            return conatiner.put((K) key, (V) value);
        } else {
            return null;
        }
    }

    @Override
    public V remove(Object key) {
        if(event.apply(EventType.REMOVE, new EventItem<K, V>(this, (K)key))) {
            return conatiner.remove(key);
        } else {
            return null;
        }
    }

    @Override
    public void putAll(Map m) {
        if(event.apply(EventType.PUT_ALL, new EventItem<K, V>(this, m))) {
            conatiner.putAll(m);
        }
    }

    @Override
    public void clear() {
        if(event.apply(EventType.CLEAR, new EventItem(this))) {
            conatiner.clear();
        }
    }

    @Override
    public Set<K> keySet() {
        return conatiner.keySet();
    }

    @Override
    public Collection<V> values() {
        return conatiner.values();
    }

    @Override
    public Set<Entry<K,V>> entrySet() {
        return conatiner.entrySet();
    }

    @Override
    public String toString() {
        return conatiner.toString();
    }
}
