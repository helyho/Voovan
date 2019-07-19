    package org.voovan.tools.collection;

import org.voovan.Global;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 对象池
 *      支持超时清理,并且支持指定对象的借出和归还操作
 *      仅仅按照时间长短控制对象的存活周期
 *
 * @author helyho
 * <p>
 * Vestful Framework.
 * WebSite: https://github.com/helyho/Vestful
 * Licence: Apache v2 License
 */
public class ObjectPool<T> {

    //<ID, 缓存的对象>
    private volatile ConcurrentHashMap<Long, PooledObject<T>> objects = new ConcurrentHashMap<Long, PooledObject<T>>();
    //未解出的对象 ID
    private volatile LinkedBlockingDeque<Long> unborrowedIdList = new LinkedBlockingDeque<Long>();

    private long aliveTime = 0;
    private boolean autoRefreshOnGet = true;
    private Function<T, Boolean> destory;
    private Supplier<T> supplier = null;
    private int minSize = 0;
    private int maxSize = Integer.MAX_VALUE;
    private int interval = 1;

    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,小于等于0时为一直存活,单位:秒
     * @param autoRefreshOnGet 获取对象时刷新对象存活时间
     */
    public ObjectPool(long aliveTime, boolean autoRefreshOnGet){
        this.aliveTime = aliveTime;
        this.autoRefreshOnGet = autoRefreshOnGet;
    }

    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,单位:秒
     */
    public ObjectPool(long aliveTime){
        this.aliveTime = aliveTime;
    }

    /**
     * 构造一个对象池
     */
    public ObjectPool(){
    }

    public long getAliveTime() {
        return aliveTime;
    }

    public ObjectPool autoRefreshOnGet(boolean autoRefreshOnGet) {
        this.autoRefreshOnGet = autoRefreshOnGet;
        return this;
    }

    public int getMinSize() {
        return minSize;
    }

    public ObjectPool minSize(int minSize) {
        this.minSize = minSize;
        return this;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public ObjectPool maxSize(int maxSize) {
        this.maxSize = maxSize;
        return this;
    }

    public int getInterval() {
        return interval;
    }

    public ObjectPool interval(int interval) {
        this.interval = interval;
        return this;
    }

    /**
     * 获取对象构造函数
     *      在对象被构造工作
     * @return 对象构造函数
     */
    public Supplier getSupplier() {
        return supplier;
    }

    /**
     * 设置对象构造函数
     *      对象被构造是用的函数
     * @param supplier 对象构造函数
     * @return ObjectPool 对象
     */
    public ObjectPool supplier(Supplier supplier) {
        this.supplier = supplier;
        return this;
    }

    /**
     * 获取对象销毁函数
     *      在对象被销毁前工作
     * @return 对象销毁函数
     */
    public Function destory() {
        return destory;
    }

    /**
     * 设置对象销毁函数
     *      在对象被销毁前工作
     * @param destory 对象销毁函数
     * @return ObjectPool 对象
     */
    public ObjectPool destory(Function destory) {
        this.destory = destory;
        return this;
    }

    /**
     * 设置对象池的对象存活时间
     * @param aliveTime 对象存活时间,单位:秒
     * @return ObjectPool 对象
     */
    public ObjectPool aliveTime(long aliveTime) {
        this.aliveTime = aliveTime;
        return this;
    }

    /**
     * 生成ObjectId
     * @return 生成的ObjectId
     */
    private long genObjectId(){
        return Global.UNIQUE_ID.nextNumber();
    }

    /**
     * 是否获取对象时刷新对象存活时间
     * @return 是否获取对象时刷新对象存活时间
     */
    public boolean isAutoRefreshOnGet(){
        return autoRefreshOnGet;
    }

    /**
     * 获取池中的对象
     * @param id 对象的id
     * @return 池中的对象
     */
    public T get(long id){
        PooledObject<T> pooledObject = objects.get(id);
        if(pooledObject!=null) {
            return pooledObject.getObject();
        }else{
            return null;
        }
    }

  /**
     * 增加池中的对象
     * @param obj 增加到池中的对象
     * @return 对象的 id 值
     */
    public Long add(T obj){
        Long id = addAndBorrow(obj);
        if(id!=null) {
            unborrowedIdList.offer(id);
            return id;
        } else {
            return null;
        }
    }

    /**
     * 增加池中的对象并立刻借出
     * @param obj 增加到池中的对象
     * @return 对象的 id 值
     */
    public Long addAndBorrow(T obj){
        Objects.requireNonNull(obj, "add a null object failed");

        if(objects.size() >= maxSize){
            return null;
        }

        long id = genObjectId();
        objects.put(id, new PooledObject<T>(this, id, obj));
        return id;
    }


    /**
     * 借出这个对象
     *         如果有提供 supplier 函数, 在没有可借出对象时会构造一个新的对象, 否则返回 null
     * @return 借出的对象的 ID
     */
    public Long borrow(){
        Long id = unborrowedIdList.poll();

        if (id == null && supplier != null) {
            synchronized (objects) {
                if(objects.size() <= maxSize) {
                    id = addAndBorrow(supplier.get());
                }
            }
        }

        return id;
    }

    /**
     * 借出对象
     * @param waitTime 超时时间
     * @return 借出地对象, 超时返回 null
     */
    public Long borrow(long waitTime) throws TimeoutException {
        Long id = null;

        id = borrow();

        if (id == null) {
            try {
                id = unborrowedIdList.poll(waitTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new TimeoutException("borrow failed.");
            }
        }

        //检查是否有重复借出
        if (id != null && !objects.get(id).setBorrow(true)) {
            throw new RuntimeException("Object already borrowed");
        }

        return id;
    }

    /**
     * 归还借出的对象
     * @param id 借出对象ID
     */
    public void restitution(Long id) {
        //检查是否有重复归还
        PooledObject pooledObject = objects.get(id);
        if (!pooledObject.isRemoved() && objects.get(id).setBorrow(false)) {
            unborrowedIdList.offer(id);
        }
    }

    /**
     * 判断池中是否存在对象
     * @param id 对象的 hash 值
     * @return true: 存在, false: 不存在
     */
    public boolean contains(long id){
        return objects.containsKey(id);
    }

    /**
     * 移除池中的对象
     * @param id 对象的 hash 值
     */
    public void remove(long id){
        unborrowedIdList.remove(id);

        PooledObject pooledObject = objects.remove(id);
        pooledObject.remove();
    }

    /**
     * 获取当前对象池的大小
     * @return 对象池的大小
     */
    public int size(){
        return objects.size();
    }

    /**
     * 出借的对象数
     * @return 出借的对象数
     */
    public int borrowedSize(){
        return objects.size() - unborrowedIdList.size();
    }

    /**
     * 可用的对象数
     * @return 可用的对象数
     */
    public int avaliableSize(){
        return unborrowedIdList.size();
    }


    /**
     * 清理池中所有的对象
     */
    public synchronized void clear(){
        for(PooledObject pooledObject : objects.values()) {
            pooledObject.remove();
        }

        unborrowedIdList.clear();
        objects.clear();
    }

    /**
     * 创建ObjectPool
     * @return ObjectPool 对象
     */
    public ObjectPool create(){
        final ObjectPool finalobjectPool = this;

        Global.getHashWheelTimer().addTask(new HashWheelTask() {
            @Override
            public void run() {
                try {
                    Iterator<PooledObject<T>> iterator = objects.values().iterator();
                    while (iterator.hasNext()) {

                        if(objects.size() <= minSize){
                            return;
                        }

                        PooledObject<T> pooledObject = iterator.next();

                        //被借出的对象不进行清理
                        if(!unborrowedIdList.contains(pooledObject.getId())){
                            continue;
                        }

                        if (!pooledObject.isAlive()) {
                            if(destory!=null){
                                //如果返回 null 则 清理对象, 如果返回为非 null 则刷新对象
                                if(destory.apply(pooledObject.getObject())){
                                    remove(pooledObject.getId());
                                } else {
                                    pooledObject.refresh();
                                }
                            } else {
                                remove(pooledObject.getId());
                            }
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }, this.interval, true);

        return this;
    }

    /**
     * 池中缓存的对象模型
     */
    public class PooledObject<T>{
        private long lastVisiediTime;
        private long id;
        @NotSerialization
        private T object;
        @NotSerialization
        private ObjectPool objectCachedPool;
        private AtomicBoolean isBorrow = new AtomicBoolean(false);
        private AtomicBoolean isRemoved = new AtomicBoolean(false);

        public PooledObject(ObjectPool objectCachedPool, long id, T object) {
            this.objectCachedPool = objectCachedPool;
            this.lastVisiediTime = System.currentTimeMillis();
            this.id = id;
            this.object = object;
        }

        protected boolean setBorrow(Boolean isBorrow) {
            return this.isBorrow.compareAndSet(!isBorrow, isBorrow);
        }

        protected boolean isBorrow() {
            return isBorrow.get();
        }

        public boolean remove() {
            return this.isRemoved.compareAndSet(false, true);
        }

        public boolean isRemoved() {
            return isRemoved.get();
        }

        /**
         * 刷新对象
         */
        public void refresh() {
            lastVisiediTime = System.currentTimeMillis();
        }

        /**
         * 获取对象
         * @return 池中的对象
         */
        public T getObject() {
            if(objectCachedPool.isAutoRefreshOnGet()) {
                refresh();
            }
            return object;
        }

        /**
         * 设置设置对象
         * @param object 池中的对象
         */
        public void setObject(T object) {
            this.object = object;
        }

        /**
         * 缓存的 id
         * @return 缓存的 id
         */
        public Long getId() {
            return id;
        }

        /**
         * 判断对象是否存活
         * @return true: 对象存活, false: 对象超时
         */
        public boolean isAlive(){
            if(objectCachedPool.aliveTime<=0){
                return true;
            }

            long currentAliveTime = System.currentTimeMillis() - lastVisiediTime;
            if (objectCachedPool.aliveTime>0 && currentAliveTime >= objectCachedPool.aliveTime*1000){
                return false;
            }else{
                return true;
            }
        }

        public String toString(){
            return JSON.toJSON(this).replace("\"","");
        }
    }

    public String toString(){
        return "{Total:" + objects.size() + ", unborrow:" + unborrowedIdList.size()+"}";
    }
}

