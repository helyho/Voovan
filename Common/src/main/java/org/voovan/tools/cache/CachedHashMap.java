package org.voovan.tools.cache;

import org.voovan.Global;
import org.voovan.tools.CollectionSearch;
import org.voovan.tools.TEnv;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.json.annotation.NotJSON;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
public class CachedHashMap<K,V> extends ConcurrentHashMap<K,V> implements CacheMap<K, V>{
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
    public CachedHashMap(Integer maxSize){
        cacheMark = new ConcurrentHashMap<K, TimeMark>();
        this.maxSize = maxSize == null ? DEFAULT_SIZE : maxSize;
    }

    /**
     * 构造函数
     */
    public CachedHashMap(){
        cacheMark = new ConcurrentHashMap<K, TimeMark>();
        this.maxSize =  DEFAULT_SIZE;
    }


    private void createCache(K key, Function<K, V> supplier){
        if(supplier==null){
           return;
        }

        CachedHashMap cachedHashMap = this;

        TimeMark timeMark = null;

        synchronized (cacheMark) {
            timeMark = cacheMark.get(key);

            if (timeMark == null) {
                timeMark = new TimeMark(this, key, Integer.MAX_VALUE);
                cacheMark.put(key, timeMark);
            }
        }

        synchronized (timeMark.createFlag) {
            if (!timeMark.isOnCreate()) {
                timeMark.tryLockOnCreate();

                //更新缓存数据, 异步
                if (asyncBuild) {
                    TimeMark finalTimeMark = timeMark;
                    Global.getThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (supplier) {
                                V value = supplier.apply(key);
                                finalTimeMark.refresh(true);
                                cachedHashMap.put(key, value);
                            }
                            finalTimeMark.releaseCreateLock();
                        }
                    });
                }
                //更新缓存数据, 异步
                else {
                    synchronized (supplier) {
                        V value = supplier.apply(key);
                        timeMark.refresh(true);
                        cachedHashMap.put(key, value);
                    }
                    timeMark.releaseCreateLock();
                }
            }
        }

        while(timeMark.isOnCreate()){
            TEnv.sleep(TimeUnit.NANOSECONDS, 1);
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
                if(!cachedHashMap.getCacheMark().isEmpty()) {
                    //清理过期的
                    for (TimeMark timeMark : (TimeMark[]) cachedHashMap.getCacheMark().values().toArray(new TimeMark[0])) {
                        if (timeMark.isExpire()) {
                            if (autoRemove) {
                                cachedHashMap.remove(timeMark.getKey());
                                cachedHashMap.cacheMark.remove(timeMark.getKey());
                            } else if (cachedHashMap.getSupplier() != null) {
                                cachedHashMap.createCache(timeMark.getKey(), cachedHashMap.supplier);
                                timeMark.refresh(true);
                            }
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
        if(cacheMark.containsKey(key) &&
                !cacheMark.get(key).isExpire() &&
                !cacheMark.get(key).isOnCreate()) {
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
        if(cacheMark.containsKey(key) &&
                !cacheMark.get(key).isExpire() &&
                !cacheMark.get(key).isOnCreate()) {
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
        if(cacheMark.containsKey(key) &&
                !cacheMark.get(key).isExpire() &&
                !cacheMark.get(key).isOnCreate()) {
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
        if(cacheMark.containsKey(key) &&
                !cacheMark.get(key).isExpire() &&
                !cacheMark.get(key).isOnCreate()) {
            cacheMark.get(key).refresh(true);
        } else {
            //如果不存在则重读
            createCache((K)key,appointedSupplier);
        }

        return this.get(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m){
        putAll(m, Integer.MAX_VALUE);
    }

    /**
     * 写入特定的整个 Map
     * @param m Map对象
     * @param expire 超时时间
     */
    public void putAll(Map<? extends K, ? extends V> m, int expire){
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            cacheMark.put(e.getKey(), new TimeMark(this, e.getKey(), expire));
        }

        super.putAll(m);
        fixSize();
    }

    @Override
    public V put(K key, V value){
        put(key, value, Integer.MAX_VALUE);
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
    public V put(K key, V value, int expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        cacheMark.putIfAbsent(key, new TimeMark(this, key, expire));
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
        return putIfAbsent(key, value, Integer.MAX_VALUE);
    }

    /**
     * 写入如果对象为空
     * @param key  键
     * @param value 值
     * @param expire 超时时间
     * @return
     */
    public V putIfAbsent(K key, V value, int expire){
        if (key == null || value == null){
            throw new NullPointerException();
        }

        V result = super.putIfAbsent(key, value);
        if(result!=null){
            return result;
        } else {
            cacheMark.putIfAbsent(key, new TimeMark(this, key, expire));
            fixSize();
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
            TimeMark[] removedTimeMark = (TimeMark[]) CollectionSearch.newInstance(cacheMark.values()).addCondition("expireTime", CollectionSearch.Operate.NOT_EQUAL, Integer.MAX_VALUE)
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
    public void expire(K key, int expire) {
        TimeMark timeMark = cacheMark.get(key);
        if(timeMark==null && !cacheMark.containsKey(key)){
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
        @NotJSON
        private CachedHashMap<K,V> mainMap;
        private K key;
        //超时时间
        private AtomicInteger expireTime = new AtomicInteger(0);
        //最后访问时间
        private AtomicLong lastTime = new AtomicLong(0);
        //访问次数
        private volatile AtomicLong visitCount = new AtomicLong(0);

        //是否正在生成数据
        private volatile AtomicBoolean createFlag = new AtomicBoolean(false);

        public TimeMark(CachedHashMap<K,V> mainMap, K key, int expireTime){
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
                    System.currentTimeMillis() - lastTime.get() >= expireTime.get()){
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

        public int getExpireTime() {
            return expireTime.get();
        }

        public void setExpireTime(int expireTime) {
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
