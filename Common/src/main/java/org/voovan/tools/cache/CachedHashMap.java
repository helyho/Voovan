package org.voovan.tools.cache;

import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
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

    private ConcurrentHashMap<K, TimeMark> cacheTimeMark;

    public CachedHashMap(){
        cacheTimeMark = new ConcurrentHashMap<K, TimeMark>();
    }

    public ConcurrentHashMap<K, TimeMark> getCacheTimeMark() {
        return cacheTimeMark;
    }

    public V getAndRefresh(K key){
        cacheTimeMark.get(key).refresh();
        return this.get(key);
    }

    public void putAll(Map<? extends K, ? extends V> m, long expire){
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            cacheTimeMark.put(e.getKey(), new TimeMark(this, e.getKey(), expire));
        }

        super.putAll(m);
    }

    public V put(K key, V value, long expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        cacheTimeMark.put(key, new TimeMark(this, key, expire));
        return super.put(key, value);
    }


    public boolean expire(K key, long expire) {
        TimeMark timeMark = cacheTimeMark.get(key);
        if(timeMark==null){
            return false;
        } else {
            timeMark.setExpireTime(expire);
            return true;
        }
    }

    public V putIfAbsent(K key, V value, long expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        V result = super.putIfAbsent(key, value);
        if(result!=null){
            cacheTimeMark.putIfAbsent(key, new TimeMark(this, key, expire));
            return result;
        } else {
            return null;
        }
    }

    @Override
    public V remove(Object key){
        cacheTimeMark.remove(key);
        return super.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value){
        cacheTimeMark.remove(key);
        return super.remove(key, value);
    }

    @Override
    public void clear() {
        cacheTimeMark.clear();
        super.clear();
    }

    /**
     * 缓存时间标签类
     */
    private class TimeMark<K> {
        private CachedHashMap<K,V> mainMap;
        private K key;
        private Long expireTime;
        private Long lastUpdateTime;

        public TimeMark(CachedHashMap<K,V> mainMap, K key, long expireTime){
            this.key = key;
            this.mainMap = mainMap;
            refresh();

            //处理缓存超时问题
            wheelTimer.addTask(new HashWheelTask() {
                @Override
                public void run() {
                    for(TimeMark timeMark : mainMap.getCacheTimeMark().values().toArray(new TimeMark[0])){
                        if(timeMark.isExpire()){
                            mainMap.remove(key);
                        }
                    }
                }
            }, 1, false);
        }

        /**
         * 刷新缓存
         */
        public synchronized void refresh(){
            this.lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * 是否过期
         * @return true: 已过期, false: 未过期
         */
        public boolean isExpire(){
            if(System.currentTimeMillis() - lastUpdateTime >= lastUpdateTime){
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
            return expireTime;
        }

        public void setExpireTime(Long expireTime) {
            synchronized (expireTime) {
                this.expireTime = expireTime;
            };
        }

        public Long getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
}
