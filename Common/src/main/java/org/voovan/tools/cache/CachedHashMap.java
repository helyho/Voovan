package org.voovan.tools.cache;

import org.voovan.Global;
import org.voovan.tools.CollectionSearch;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.json.annotation.NotJSON;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * 进程内缓存处理类
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class CachedHashMap<K,V> extends ConcurrentHashMap<K,V>{
    public static int DEFAULT_SIZE = 1000;

    protected final static HashWheelTimer wheelTimer = new HashWheelTimer(1000, 1);
    private Function<K, V> supplier = null;
    private boolean asyncBuild = true;
    private int interval = 1000;
    private boolean autoRemove = true;

    static {
        wheelTimer.rotate();
    }

    private ConcurrentHashMap<K, TimeMark> cacheMark;
    private int maxSize;

    /**
     * 构造函数
     * @param maxSize 缓存集合的最大容量, 多余的数据会被移除
     */
    private CachedHashMap(Integer maxSize){
        cacheMark = new ConcurrentHashMap<K, TimeMark>();
        this.maxSize = maxSize == null ? DEFAULT_SIZE : maxSize;
    }

    private void createCache(K key, Function<K, V> supplier){
        CachedHashMap cachedHashMap = this;
        //更新缓存数据, 异步
        if (asyncBuild) {
            Global.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    if(supplier !=null) {
                        synchronized (supplier) {
                            V value = supplier.apply(key);
                            cachedHashMap.put(key, value);
                        }
                    }
                }
            });
        }
        //更新缓存数据, 异步
        else {
            if(supplier !=null) {
                synchronized (supplier) {
                    V value = supplier.apply(key);
                    cachedHashMap.put(key, value);
                }
            }
        }
    }

    /**
     * 设置数据创建 Function 对象
     * @param buildFunction Function 对象
     * @param asyncBuild 异步构造数据
     */
    public void supplier(Function<K, V> buildFunction, boolean asyncBuild){
        this.supplier = buildFunction;
        this.asyncBuild = asyncBuild;
        this.autoRemove = false;
    }

    /**
     * 设置数据创建 Function 对象
     * @param buildFunction Function 对象
     * @return CachedHashMap 对象
     */
    public CachedHashMap<K, V> supplier(Function<K, V> buildFunction){
        supplier(buildFunction, true);
        this.autoRemove = false;
        return this;
    }

    /**
     * 设置最大容量
     * @param maxSize 最大容量
     * @return CachedHashMap 对象
     */
    public CachedHashMap<K, V> maxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    /**
     * 设置最失效对象检查周期
     * @param interval 检查周期
     * @return CachedHashMap 对象
     */
    public CachedHashMap<K, V> interval(int interval) {
        this.interval = interval;
        return this;
    }

    /**
     * 设置失效是否自动移除
     * @param autoRemove true: 自动移除, false: 并不自动移除
     * @return CachedHashMap 对象
     */
    public CachedHashMap<K, V> autoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
        return this;
    }


    public CachedHashMap<K, V> create(){
        final CachedHashMap cachedHashMap = this;
        wheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                //清理过期的
                for(TimeMark timeMark : (TimeMark[])cachedHashMap.getCacheMark().values().toArray(new TimeMark[0])){
                    if(timeMark.isExpire()){
                        if(autoRemove) {
                            cachedHashMap.remove(timeMark.getKey());
                            cachedHashMap.cacheMark.remove(timeMark.getKey());
                        } else if(cachedHashMap.getSupplier() != null){
                            cachedHashMap.createCache(timeMark.getKey(), cachedHashMap.supplier);
                            timeMark.refresh(true);
                        }
                    }
                }
            }
        }, interval);

        return this;
    }

    /**
     * 获取数据创建 Function 对象
     * @return Function 对象
     */
    protected Function<K, V> getSupplier(){
        return supplier;
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
        if(cacheMark.containsKey(key) && !cacheMark.get(key).isExpire()) {
            cacheMark.get(key).refresh(false);
        } else {
            //如果不存在则重读
            createCache((K)key, this.supplier);
        }
        return super.get(key);
    }


    /**
     * 获取对象
     *      不会刷新对象的时间戳
     * @param key 键
     * @param appointedSupplier 指定的获取器
     * @return 值
     */
    public V get(Object key, Function<K, V> appointedSupplier){
        if(cacheMark.containsKey(key) && !cacheMark.get(key).isExpire()) {
            cacheMark.get(key).refresh(false);
        } else {
            //如果不存在则重读
            createCache((K)key,appointedSupplier);
        }
        return super.get(key);
    }


    /**
     * 获取并刷新对象
     * @param key 键
     * @return 值
     */
    public V getAndRefresh(Object key){
        if(cacheMark.contains(key) && !cacheMark.get(key).isExpire()) {
            cacheMark.get(key).refresh(true);
        } else {
            //如果不存在则重读
            createCache((K)key, this.supplier);
        }
        return this.get(key);
    }


    /**
     * 获取并刷新对象
     * @param key 键
     * @return 值
     */
    public V getAndRefresh(Object key, Function<K, V> appointedSupplier){
        if(cacheMark.contains(key) && !cacheMark.get(key).isExpire()) {
            cacheMark.get(key).refresh(true);
        } else {
            //如果不存在则重读
            createCache((K)key,appointedSupplier);
        }
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
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
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
     * 是否过期
     * @param key 是否过期
     * @return true: 过期, false: 未过去
     */
    public boolean isExpire(String key){
        return cacheMark.get(key).isExpire();
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
        return putIfAbsent(key, value, Long.MAX_VALUE);
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
        int diffSize = this.size() - maxSize;
        if (diffSize > 0) {
            //最少访问次数中, 时间最老的进行清楚
            TimeMark[] removedTimeMark = (TimeMark[]) CollectionSearch.newInstance(cacheMark.values()).addCondition("expireTime", CollectionSearch.Operate.NOT_EQUAL, Long.MAX_VALUE)
                    .addCondition("lastTime", CollectionSearch.Operate.LESS, System.currentTimeMillis()-1000)
                    .sort("visitCount")
                    .limit(10 * diffSize)
                    .sort("lastTime")
                    .limit(diffSize)
                    .search()
                    .toArray(new TimeMark[0]);
            for (TimeMark timeMark : removedTimeMark) {
                System.out.println("remove");
                cacheMark.remove(timeMark.getKey());
                this.remove(timeMark.getKey());
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

    public static <K, V> CachedHashMap<K, V> newInstance(){
        return new CachedHashMap<K, V>(null);
    }

    public static <K, V> CachedHashMap<K, V> newInstance(int maxSize){
        return new CachedHashMap<K, V>(maxSize);
    }

    /**
     * 缓存时间标签类
     */
    private class TimeMark<K> {
        @NotJSON
        private CachedHashMap<K,V> mainMap;
        private K key;
        //超时时间
        private AtomicLong expireTime = new AtomicLong(0);
        //最后访问时间
        private AtomicLong lastTime = new AtomicLong(0);
        //访问次数
        private AtomicLong visitCount = new AtomicLong(0);

        //是否正在生成数据
        private AtomicBoolean isCreating = new AtomicBoolean(false);

        public TimeMark(CachedHashMap<K,V> mainMap, K key, long expireTime){
            this.key = key;
            this.mainMap = mainMap;
            this.expireTime.set(expireTime);
            visitCount.set(0);
            refresh(true);
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
            if(expireTime.get()!=Long.MAX_VALUE &&
                    System.currentTimeMillis() - lastTime.get() >= expireTime.get()*1000){
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

        public synchronized boolean setIsCreating(boolean isCreating){
            return this.isCreating.compareAndSet(!isCreating, isCreating);
        }
    }
}
