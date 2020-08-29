package org.voovan.tools.collection;

import org.voovan.tools.TEnv;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 进程内缓存处理类
 *      可字发现对象,在对象没有时自动同步对象到缓存,具备超时, 按照 LRU 的原则清理过期数据
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class CacheMap<K,V> implements ICacheMap<K, V> {

    protected final static HashWheelTimer CACHE_MAP_WHEEL_TIMER = new HashWheelTimer("CacheMap", 60, 1000);
    private Function<K, V> supplier = null;
    private int interval = 30;
    private boolean autoRemove = true;
    private BiFunction<K, V, Long> destory;

    private long expire = 0;

    static {
        CACHE_MAP_WHEEL_TIMER.rotate();
    }

    private Map<K, V> cacheData = null;
    private ConcurrentHashMap<K, TimeMark> cacheMark = new ConcurrentHashMap<K, TimeMark>();
    private int maxSize;

    /**
     * 构造函数
     * @param map 缓存数据的 Map 对象
     * @param maxSize 最大元素数量
     */
    public CacheMap(Map<K, V> map, int maxSize){
        this.maxSize = maxSize;
        this.cacheData = map;
    }

    public CacheMap(Map<K, V> map){
        this.maxSize = Integer.MAX_VALUE;
        this.cacheData = map;
    }

    /**
     * 构造函数
     * @param maxSize 缓存集合的最大容量, 多余的数据会被移除
     */
    public CacheMap(Integer maxSize){
        this.maxSize = maxSize == null ? Integer.MAX_VALUE : maxSize;
        this.cacheData = new ConcurrentHashMap<K, V>(maxSize);
    }

    /**
     * 构造函数
     */
    public CacheMap(){
        this.maxSize = Integer.MAX_VALUE;
        this.cacheData = new ConcurrentHashMap<K, V>();
    }

    /**
     * 获取数据创建 Function 对象
     * @return Function 对象
     */
    public Function<K, V> getSupplier(){
        return supplier;
    }

    /**
     * 设置数据创建 Function 对象, 默认数据失效时不移除数据
     * @param buildFunction Function 对象
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> supplier(Function<K, V> buildFunction){
        this.supplier = buildFunction;
        this.autoRemove = false;
        return this;
    }

    /**
     * 设置数据创建 Function 对象
     * @param buildFunction Function 对象
     * @param autoRemove 设置失效是否自动移除
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> supplier(Function<K, V> buildFunction, boolean autoRemove){
        this.supplier = buildFunction;
        this.autoRemove = autoRemove;
        return this;
    }

    /**
     * 获取对象销毁函数
     * @return 对象销毁函数
     */
    public BiFunction<K, V, Long> getDestory() {
        return destory;
    }

    /**
     * 设置对象销毁函数
     * @param destory 对象销毁函数,
     *                1.返回 null 则刷新为默认超时时间
     *                2.小于0 的数据, 则移除对象
     *                3.大于0的数据则重新设置返回值为新的超时时间
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> destory(BiFunction<K, V, Long> destory) {
        this.destory = destory;
        return this;
    }

    /**
     * 设置最大容量
     * @param maxSize 最大容量
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> maxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    /**
     * 设置最失效对象检查周期
     * @param interval 检查周期, 单位:秒, 小于零不做超时处理
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> interval(int interval) {
        this.interval = interval;
        return this;
    }

    /**
     * 设置失效是否自动移除
     * @param autoRemove true: 自动移除, false: 并不自动移除
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> autoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
        return this;
    }

    /**
     * 获取默认超时时间
     * @return 获取超时时间
     */
    public long getExpire() {
        return expire;
    }

    /**
     * 设置默认超时时间
     * @param expire 超时时间
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> expire(long expire) {
        this.expire = expire;
        return this;
    }

    /**
     * 获取缓存标记对象
     * @return 缓存数据标记的键值对
     */
    public ConcurrentHashMap<K, TimeMark> getCacheMark() {
        return cacheMark;
    }

    /**
     * 创建CachedHashMap
     * @return CachedHashMap 对象
     */
    public CacheMap<K, V> create(){
        final CacheMap cacheMap = this;

        //启动超时清理任务
        if(interval >= 1) {
            CACHE_MAP_WHEEL_TIMER.addTask(new HashWheelTask() {
                @Override
                public void run() {
                    if (!cacheMap.getCacheMark().isEmpty()) {
                        //清理过期的
                        for (TimeMark timeMark : (TimeMark[]) cacheMap.getCacheMark().values().toArray(new TimeMark[0])) {
                            cacheMap.checkAndDoExpire(timeMark);
                        }
                        fixSize();
                    }
                }
            }, interval);
        }
        return this;
    }

    /**
     * 检查指定的Key, 如果过期则处理过期的数据
     * @param key 检查的 Key
     */
    public void checkAndDoExpire(K key){
        checkAndDoExpire(cacheMark.get(key));
    }

    /**
     * 检查或处理过期的数据
     * @param timeMark 检查的 TimeMark 对象
     * @return true: 已过期, false: 未过期, 有可能已经通过 supplier 重置
     */
    private boolean checkAndDoExpire(TimeMark<K> timeMark){
        if (timeMark!=null && timeMark.isExpire()) {
            if (autoRemove) {
                if(destory!= null) {

                    V data = cacheData.get(timeMark.getKey());
                    if(data == null) {
                        this.remove(timeMark.getKey());
                    }

                    // 1.返回 null 则刷新为默认超时时间
                    // 2.小于0 的数据, 则移除对象
                    // 3.大于0的数据则重新设置返回值为新的超时时间
                    Long value = destory.apply(timeMark.getKey(), data);
                    if(value == null){
                        timeMark.refresh(true);
                    } else if (value < 0) {
                        remove(timeMark.getKey());
                    } else {
                        timeMark.setExpireTime(value);
                    }
                } else {
                    remove(timeMark.getKey());
                }

            } else if (getSupplier() != null) {
                if(createCache(timeMark.getKey(), supplier, timeMark.getExpireTime())) {
                    timeMark.refresh(true);
                    return false;
                } else {
                    return true;
                }
            }

            return true;
        }

        return false;
    }

    private boolean createCache(K key, Function<K, V> supplier, Long createExpire){
        if(supplier==null){
            return false;
        }

        try {
            V value = supplier.apply(key);
            if(value == null) {
                remove(key);
                return false;
            }

            if(expire==Long.MAX_VALUE) {
                this.put(key, value);
            } else {
                this.put(key, value, createExpire);
            }

            return true;
        } catch (Exception e){
            Logger.error("Create with supplier failed: ", e);
            return false;
        }

    }

    /**
     * 获取对象
     *      不会刷新对象的时间戳
     * @param key 键
     * @param appointedSupplier 指定的获取器
     * @param createExpire 超时时间
     * @param refresh 刷新超时时间
     * @return 值
     */
    @Override
    public V get(Object key, Function<K, V> appointedSupplier, Long createExpire, boolean refresh){

        TimeMark timeMark = cacheMark.get(key);

        appointedSupplier = appointedSupplier == null ? supplier : appointedSupplier;
        createExpire = createExpire == null ? expire : createExpire;

        if (timeMark == null) {
            synchronized (cacheMark) {
                timeMark = cacheMark.get(key);
                if(timeMark==null) {
                    if(createCache((K) key, appointedSupplier, createExpire)) {
                        timeMark = new TimeMark(this, key, createExpire);
                        cacheMark.put((K) key, timeMark);
                    }
                }
            }
        } else {
            if (!timeMark.isExpire()) {
                timeMark.refresh(refresh);
            } else {
                if(checkAndDoExpire(timeMark)){
                    if(appointedSupplier!=null) {
                        timeMark.refresh(true);
                        createCache((K) key, appointedSupplier, createExpire);
                    }
                }
            }
        }

        return cacheData.get(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m){
        putAll(m, expire);
    }

    /**
     * 写入特定的整个 Map
     * @param m Map对象
     * @param expire 超时时间
     */
    public void putAll(Map<? extends K, ? extends V> m, long expire){
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            cacheMark.put(e.getKey(), new TimeMark(this, e.getKey(), expire));
        }

        cacheData.putAll(m);
    }

    @Override
    public int size() {
        return cacheData.size();
    }

    @Override
    public boolean isEmpty() {
        return cacheData.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cacheData.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cacheData.containsValue(value);
    }

    @Override
    public V put(K key, V value){
        put(key, value, expire);
        return value;
    }

    /**
     * 写入对象
     * @param key  键
     * @param value 值
     * @param expire 超时时间
     * @return 被置入的对象
     */
    public V put(K key, V value, long expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        cacheMark.putIfAbsent(key, new TimeMark(this, key, expire));
        return cacheData.put(key, value);
    }

    /**
     * 写入如果对象为空
     * @param key  键
     * @param value 值
     * @return 如果数据存在返回已经存在对象, 如果数据不存在,新的对象被置入,则返回: null
     */
    public V putIfAbsent(K key, V value){
        return putIfAbsent(key, value, expire);
    }

    /**
     * 写入如果对象为空
     * @param key  键
     * @param value 值
     * @param expire 超时时间
     * @return 如果数据存在返回已经存在对象, 如果数据不存在,新的对象被置入,则返回: null
     */
    public V putIfAbsent(K key, V value, long expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        V result = cacheData.putIfAbsent(key, value);
        cacheMark.putIfAbsent(key, new TimeMark(this, key, expire));

        if(result!=null){
            return result;
        } else {
            return null;
        }
    }


    /**
     * 是否过期
     * @param key 是否过期
     * @return true: 过期, false: 未过去
     */
    public boolean isExpire(String key){
        return cacheMark.get(key).isExpire();
    }

    /**
     * 清理过多的数据记录
     */
    private void fixSize() {
        //如果超出容量限制
        int diffSize = this.size() - maxSize;
        if (diffSize > 0) {
            //最少访问次数中, 时间最老的进行清除
            TimeMark<K>[] removedTimeMark = (TimeMark<K>[]) CollectionSearch.newInstance(cacheMark.values())
                    .setParallelStream(true)
                    .addCondition("expireTime", CollectionSearch.Operate.NOT_EQUAL, 0L)
                    .addCondition("lastTime", CollectionSearch.Operate.LESS, System.currentTimeMillis()-1000)
                    .sort("visitCount")
                    .limit(10 * diffSize)
                    .sort("lastTime")
                    .limit(diffSize)
                    .search()
                    .toArray(new TimeMark[0]);
            for (TimeMark<K> timeMark : removedTimeMark) {
                if(destory!=null) {

                    V data = cacheData.get(timeMark.getKey());
                    if(data == null) {
                        this.remove(timeMark.getKey());
                        continue;
                    }

                    Long value = destory.apply(timeMark.getKey(), data);

                    if(value == null){
                        timeMark.refresh(true);
                    } else if (value < 0) {
                        remove(timeMark.getKey());
                    } else {
                        timeMark.setExpireTime(value);
                    }
                } else {
                    this.remove(timeMark.getKey());
                }
            }
        }
    }

    @Override
    public long getTTL(K key) {
        TimeMark timeMark = cacheMark.get(key);
        if(timeMark!=null) {
            return timeMark.getExpireTime();
        } else {
            return -1;
        }
    }

    /**
     * 更新某个对象的超时时间
     *      可以为某个没有配置超时时间的键值对配置超时时间
     * @param key 键
     * @param expire 超时时间
     */
    @Override
    public boolean setTTL(K key, long expire) {

        TimeMark timeMark = cacheMark.get(key);
        if(timeMark==null){
            return false;
        } else {
            timeMark.setExpireTime(expire);
            timeMark.refresh(true);
            return true;
        }
    }

    @Override
    public V remove(Object key){
        cacheMark.remove(key);
        return cacheData.remove(key);
    }

    @Override
    public boolean remove(Object key, Object value){
        cacheMark.remove(key);
        return cacheData.remove(key, value);
    }

    @Override
    public void clear() {
        cacheMark.clear();
        cacheData.clear();
    }

    @Override
    public Set<K> keySet() {
        return cacheData.keySet();
    }

    @Override
    public Collection<V> values() {
        return cacheData.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return cacheData.entrySet();
    }

    /**
     * 缓存时间标签类
     */
    private class TimeMark<K> {
        @NotSerialization
        private CacheMap<K,V> mainMap;
        private K key;
        //超时时间
        private AtomicLong expireTime = new AtomicLong(0);
        //最后访问时间
        private AtomicLong lastTime = new AtomicLong(0);
        //访问次数
        private AtomicLong visitCount = new AtomicLong(0);

        //是否正在生成数据
        private AtomicBoolean createFlag = new AtomicBoolean(false);

        public TimeMark(CacheMap<K,V> mainMap, K key, long expireTime){
            this.key = key;
            this.mainMap = mainMap;
            this.expireTime.set(expireTime);
            visitCount.set(0);
            refresh(true);
        }

        /**
         * 刷新缓存
         */
        public void refresh(boolean updateLastTime){
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
            if(expireTime.get()>0 && System.currentTimeMillis() - lastTime.get() >= expireTime.get()*1000){
                return true;
            } else {
                return false;
            }
        }

        public CacheMap<K, V> getMainMap() {
            return mainMap;
        }

        public K getKey() {
            return key;
        }

        public long getExpireTime() {
            return expireTime.get();
        }

        public void setExpireTime(long expireTime) {
            this.expireTime.set(expireTime);
        }

        public Long getLastTime() {
            return lastTime.get();
        }

        public AtomicLong getVisitCount() {
            return visitCount;
        }

        public boolean isOnCreate(){
            return createFlag.get();
        }

        public boolean tryLockOnCreate() {
            return createFlag.compareAndSet(false, true);
        }

        public void releaseCreateLock() {
            createFlag.set(false);
        }
    }
}
