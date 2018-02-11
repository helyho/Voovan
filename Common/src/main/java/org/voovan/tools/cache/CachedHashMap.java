package org.voovan.tools.cache;

import org.voovan.tools.CollectionSearch;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内缓冲处理类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class CachedHashMap<K,V> extends ConcurrentHashMap<K,V>{

    protected final static HashWheelTimer wheelTimer = new HashWheelTimer(60, 1000);

    static {
        wheelTimer.rotate();
    }

    private ConcurrentHashMap<K, TimeMark> cacheMark;
    private int maxSize;

    /**
     * 构造函数
     * @param maxSize 缓存集合的最大容量, 多余的数据会被移除
     */
    public CachedHashMap(int maxSize){
        cacheMark = new ConcurrentHashMap<K, TimeMark>();
        this.maxSize = maxSize;

        final CachedHashMap cachedHashMap = this;
        wheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                //清理过期的
                for(TimeMark timeMark : (TimeMark[])cachedHashMap.getCacheMark().values().toArray()){
                    if(timeMark.isExpire()){
                        cachedHashMap.remove(timeMark.getKey());
                    }
                }
            }
        }, 1000);
    }

    /**
     * 获取缓存标记对象
     * @return
     */
    public ConcurrentHashMap<K, TimeMark> getCacheMark() {
        return cacheMark;
    }

    /**
     * 获取对象
     *      不会刷新对象的时间戳
     * @param key 键
     * @return 值
     */
    @Override
    public V get(Object key){
        cacheMark.get(key).refresh(false);
        return super.get(key);
    }

    /**
     * 获取并刷新对象
     * @param key 键
     * @return 值
     */
    public V getAndRefresh(Object key){
        cacheMark.get(key).refresh(true);
        return this.get(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m){
        putAll(m, Long.MAX_VALUE);
    }

    /**
     * 写入特定的整个 Map
     * @param m Map对象
     * @param expire 超时时间
     */
    public void putAll(Map<? extends K, ? extends V> m, long expire){
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
            cacheMark.put(e.getKey(), new TimeMark(this, e.getKey(), expire));
        }

        super.putAll(m);
        fixSize();
    }

    @Override
    public V put(K key, V value){
        put(key, value, Long.MAX_VALUE);
        return value;
    }

    /**
     * 写入对象
     * @param key  键
     * @param value 值
     * @param expire 超时时间
     * @return
     */
    public V put(K key, V value, long expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        cacheMark.put(key, new TimeMark(this, key, expire));
        super.put(key, value);
        fixSize();
        return value;
    }

    /**
     * 写入如果对象为空
     * @param key  键
     * @param value 值
     * @return
     */
    public V putIfAbsent(K key, V value){
        putIfAbsent(key, value, Long.MAX_VALUE);
        return value;
    }

    /**
     * 写入如果对象为空
     * @param key  键
     * @param value 值
     * @param expire 超时时间
     * @return
     */
    public V putIfAbsent(K key, V value, long expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        V result = super.putIfAbsent(key, value);
        if(result!=null){
            cacheMark.putIfAbsent(key, new TimeMark(this, key, expire));
            fixSize();
            return result;
        } else {
            return null;
        }
    }

    /**
     * 清理过多的数据记录
     */
    private void fixSize() {
        //如果超出容量限制
        int diffSize = this.maxSize - maxSize;
        if (diffSize > 0) {
            //最少访问次数中, 时间最老的进行清楚
            TimeMark[] removedTimeMark = (TimeMark[]) CollectionSearch.newInstance(cacheMark.values()).sort("visitCount").limit(10 * diffSize).sort("lastTime").limit(diffSize).search().toArray();
            for (TimeMark timeMark : removedTimeMark) {
                cacheMark.remove(timeMark.getKey());
            }
        }
    }

    /**
     * 更新某个对象的超时时间
     *      可以为某个没有配置超时时间的键值对配置超时时间
     * @param key 键
     * @param expire 超时时间
     */
    public void expire(K key, long expire) {
        TimeMark timeMark = cacheMark.get(key);
        if(timeMark==null){
            cacheMark.put(key, new TimeMark(this, key, expire));
        } else {
            timeMark.setExpireTime(expire);
        }
    }

    @Override
    public V remove(Object key){
        cacheMark.remove(key);
        return super.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value){
        cacheMark.remove(key);
        return super.remove(key, value);
    }

    @Override
    public void clear() {
        cacheMark.clear();
        super.clear();
    }

    /**
     * 缓存时间标签类
     */
    private class TimeMark<K> {
        private CachedHashMap<K,V> mainMap;
        private K key;
        private AtomicLong expireTime;
        private AtomicLong lastTime;
        private AtomicLong visitCount;

        public TimeMark(CachedHashMap<K,V> mainMap, K key, long expireTime){
            this.key = key;
            this.mainMap = mainMap;
            visitCount.set(0);
            refresh(false);
        }

        /**
         * 刷新缓存
         */
        public synchronized void refresh(boolean updateLastTime){
            visitCount.incrementAndGet();
            if(updateLastTime) {
                this.lastTime.set(System.currentTimeMillis());
            }
        }

        /**
         * 是否过期
         * @return true: 已过期, false: 未过期
         */
        public boolean isExpire(){
            if(System.currentTimeMillis() - lastTime.get() >= expireTime.get()){
                return true;
            } else {
                return false;
            }
        }

        public CachedHashMap<K, V> getMainMap() {
            return mainMap;
        }

        public K getKey() {
            return key;
        }

        public Long getExpireTime() {
            return expireTime.get();
        }

        public void setExpireTime(Long expireTime) {
            this.expireTime.set(expireTime);
        }

        public Long getLastTime() {
            return lastTime.get();
        }

        public AtomicLong getVisitCount() {
            return visitCount;
        }
    }
}
