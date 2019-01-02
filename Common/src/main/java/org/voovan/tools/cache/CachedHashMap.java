package org.voovan.tools.cache;

import org.voovan.Global;
import org.voovan.tools.CollectionSearch;
import org.voovan.tools.TEnv;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.json.annotation.NotJSON;
import org.voovan.tools.log.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
public class CachedHashMap<K,V> extends ConcurrentHashMap<K,V> implements CacheMap<K, V>{

    protected final static HashWheelTimer wheelTimer = new HashWheelTimer(60, 1000);
    private Function<K, V> supplier = null;
    private boolean asyncBuild = true;
    private int interval = 1;
    private boolean autoRemove = true;
    private Function destory;

    private long expire = 0;

    static {
        wheelTimer.rotate();
    }

    private ConcurrentHashMap<K, TimeMark> cacheMark = new ConcurrentHashMap<K, TimeMark>();
    private int maxSize;

    /**
     * 构造函数
     * @param maxSize 缓存集合的最大容量, 多余的数据会被移除
     */
    public CachedHashMap(Integer maxSize){
        this.maxSize = maxSize == null ? Integer.MAX_VALUE : maxSize;
    }

    /**
     * 构造函数
     */
    public CachedHashMap(){
        this.maxSize = Integer.MAX_VALUE;
    }

    /**
     * 获取数据创建 Function 对象
     * @return Function 对象
     */
    public Function<K, V> getSupplier(){
        return supplier;
    }

    /**
     * 设置数据创建 Function 对象
     * @param buildFunction Function 对象
     * @param asyncBuild 异步构造数据
     * @return CachedHashMap 对象
     */
    public CachedHashMap<K, V> supplier(Function<K, V> buildFunction, boolean asyncBuild){
        this.supplier = buildFunction;
        this.asyncBuild = asyncBuild;
        this.autoRemove = false;
        return this;
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
     * 获取对象销毁函数
     * @return 对象销毁函数
     */
    public Function getDestory() {
        return destory;
    }

    /**
     * 设置对象销毁函数
     * @param destory 对象销毁函数, 如果返回 null 则 清理对象, 如果返回为非 null 则刷新对象
     * @return CachedHashMap 对象
     */
    public CachedHashMap<K, V> destory(Function destory) {
        this.destory = destory;
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
     * @param interval 检查周期, 单位:毫秒, 小于零不做超时处理
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
    public CachedHashMap expire(long expire) {
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
    public CachedHashMap<K, V> create(){
        final CachedHashMap cachedHashMap = this;

        //启动超时清理任务
        if(interval >= 1) {
            wheelTimer.addTask(new HashWheelTask() {
                @Override
                public void run() {
                    if (!cachedHashMap.getCacheMark().isEmpty()) {
                        //清理过期的
                        for (TimeMark timeMark : (TimeMark[]) cachedHashMap.getCacheMark().values().toArray(new TimeMark[0])) {
                            if (timeMark.isExpire()) {
                                if (autoRemove) {
                                    if(destory!= null) {
                                        //如果返回 null 则 清理对象, 如果返回为非 null 则 刷新对象
                                        if (destory.apply(cachedHashMap.get(timeMark.key)) == null) {
                                            cachedHashMap.remove(timeMark.getKey());
                                            cachedHashMap.cacheMark.remove(timeMark.getKey());
                                        } else {
                                            timeMark.refresh(true);
                                        }
                                    } else {
                                        cachedHashMap.cacheMark.remove(timeMark.getKey());
                                        cachedHashMap.remove(timeMark.getKey());
                                    }

                                } else if (cachedHashMap.getSupplier() != null) {
                                    cachedHashMap.createCache(timeMark.getKey(), cachedHashMap.supplier, timeMark.getExpireTime());
                                    timeMark.refresh(true);
                                }
                            }
                        }
                        fixSize();
                    }
                }
            }, interval);
        }
        return this;
    }



    private void createCache(K key, Function<K, V> supplier, Long createExpire){
        if(supplier==null){
            return;
        }

        CachedHashMap cachedHashMap = this;

        TimeMark timeMark = null;

        synchronized (cacheMark) {
            timeMark = cacheMark.get(key);

            if (timeMark == null) {
                timeMark = new TimeMark(this, key, createExpire);
                cacheMark.put(key, timeMark);
            }
        }

        TimeMark finalTimeMark = timeMark;
        Long finalCreateExpire = createExpire;

        synchronized (timeMark.createFlag) {
            if (!timeMark.isOnCreate()) {

                //更新缓存数据, 异步
                if (asyncBuild) {
                    Global.getThreadPool().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                synchronized (supplier) {
                                    finalTimeMark.tryLockOnCreate();
                                    V value = supplier.apply(key);
                                    finalTimeMark.refresh(true);

                                    if(value != null) {
                                        if (expire == Long.MAX_VALUE) {
                                            cachedHashMap.put(key, value);
                                        } else {
                                            cachedHashMap.put(key, value, finalCreateExpire);
                                        }
                                    }

                                }
                            } catch (Exception e) {
                                Logger.error("CacheHashMap create value failed: ", e);
                            } finally {
                                finalTimeMark.releaseCreateLock();
                            }
                        }
                    });
                }
                //更新缓存数据, 异步
                else {
                    try{
                        synchronized (supplier) {
                            timeMark.tryLockOnCreate();
                            V value = supplier.apply(key);
                            timeMark.refresh(true);

                            if(value != null) {
                                if (expire == Long.MAX_VALUE) {
                                    cachedHashMap.put(key, value);
                                } else {
                                    cachedHashMap.put(key, value, finalCreateExpire);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.error("CacheHashMap create value failed: ", e);
                    } finally {
                        finalTimeMark.releaseCreateLock();
                    }
                }
            }
        }

        TEnv.wait(()-> finalTimeMark.isOnCreate());
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
        if(cacheMark.containsKey(key) &&
                !cacheMark.get(key).isExpire() &&
                !cacheMark.get(key).isOnCreate()) {
            cacheMark.get(key).refresh(refresh);
        } else {
            appointedSupplier = appointedSupplier==null ? supplier : appointedSupplier;
            createExpire = createExpire==null ? expire : createExpire;
            createCache((K)key, appointedSupplier, createExpire);
        }

        return super.get(key);
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

        super.putAll(m);
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
        return super.put(key, value);
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

        V result = super.putIfAbsent(key, value);
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
            //最少访问次数中, 时间最老的进行清楚
            TimeMark[] removedTimeMark = (TimeMark[]) CollectionSearch.newInstance(cacheMark.values()).addCondition("expireTime", CollectionSearch.Operate.NOT_EQUAL, 0L)
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

    @Override
    public long getTTL(K key) {
        return cacheMark.get(key).getExpireTime();
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
        if(timeMark==null && !cacheMark.containsKey(key)){
            return false;
        } else {
            timeMark.setExpireTime(expire);
            timeMark.refresh(true);
        }

        return true;
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
        private AtomicLong expireTime = new AtomicLong(0);
        //最后访问时间
        private AtomicLong lastTime = new AtomicLong(0);
        //访问次数
        private volatile AtomicLong visitCount = new AtomicLong(0);

        //是否正在生成数据
        private volatile AtomicBoolean createFlag = new AtomicBoolean(false);

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

        public CachedHashMap<K, V> getMainMap() {
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
