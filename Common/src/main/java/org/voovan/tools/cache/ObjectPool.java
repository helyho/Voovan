package org.voovan.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TEnv;
import org.voovan.tools.TString;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.annotation.NotJSON;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
public class ObjectPool {

    private volatile ConcurrentHashMap<Object, PooledObject> objects = new ConcurrentHashMap<Object, PooledObject>();
    private volatile ConcurrentLinkedDeque<Object> unborrowedObjectIdList  = new ConcurrentLinkedDeque<Object>();

    private long aliveTime = 0;
    private boolean autoRefreshOnGet = true;
    private Function destory;
    private Supplier supplier = null;
    private int minSize = 0;
    private int maxSize = Integer.MAX_VALUE;
    private int interval = 5;

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
    private String genObjectId(){
        return TString.generateShortUUID();
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
     * @param id 对象的 hash 值
     * @return 池中的对象
     */
    public Object get(Object id){
        PooledObject pooledObject = objects.get(id);
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
    public Object add(Object obj){
        Object id = genObjectId();
        return add(id, obj);
    }

    /**
     * 增加池中的对象
     * @param id 增加到池中的对象ID
     * @param obj 增加到池中的对象
     * @return 对象的 id 值
     */
    public Object add(Object id, Object obj){
        if(addAndBorrow(id, obj)!=null) {
            unborrowedObjectIdList.offer(id);
            return id;
        } else {
            return null;
        }
    }

    /**
     * 增加池中的对象
     * @param id 增加到池中的对象ID
     * @param obj 增加到池中的对象
     * @return 对象的 id 值
     */
    public Object addAndBorrow(Object id, Object obj){
        if(obj == null){
            return null;
        }

        if(objects.size() >= maxSize){
            new RuntimeException("ObjectPool is full.").printStackTrace();
            return null;
        }

        objects.put(id, new PooledObject(this, id, obj));
        return id;
    }

    /**
     * 判断池中是否存在对象
     * @param id 对象的 hash 值
     * @return true: 存在, false: 不存在
     */
    public boolean contains(Object id){
        return objects.containsKey(id);
    }

    /**
     * 移除池中的对象
     * @param id 对象的 hash 值
     */
    public synchronized void remove(Object id){
        objects.remove(id);
        unborrowedObjectIdList.remove(id);
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
        return objects.size() - unborrowedObjectIdList.size();
    }

    /**
     * 可用的对象数
     * @return 可用的对象数
     */
    public int avaliableSize(){
        return unborrowedObjectIdList.size();
    }


    /**
     * 清理池中所有的对象
     */
    public synchronized void clear(){
        objects.clear();
        unborrowedObjectIdList.clear();
    }

    /**
     * 借出这个对象
     *         如果有提供 supplier 函数, 在没有可借出对象时会构造一个新的对象, 否则返回 null
     * @return 借出的对象的 ID
     */
    public Object borrow(){
        Object borrowedObject = unborrowedObjectIdList.poll();
        if(borrowedObject==null && supplier!=null){
            borrowedObject = addAndBorrow(genObjectId(), supplier.get());
        }

        return borrowedObject;
    }

    /**
     * 借出对象
     * @param waitTime 超时时间
     * @return 借出地对象, 超时返回 null
     */
    public Object borrow(int waitTime){
        Object objectId = null;
        while(waitTime>=0) {
            objectId = borrow();
            if (objectId == null) {
                TEnv.sleep(1);
            } else {
                break;
            }
        }

        return objectId;
    }


    /**
     * 归还借出的对象
     * @param id 借出对象ID
     */
    public void restitution(Object id){
        unborrowedObjectIdList.addLast(id);
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
                    Iterator<PooledObject> iterator = objects.values().iterator();
                    while (iterator.hasNext()) {

                        if(objects.size() <= minSize){
                            return;
                        }

                        PooledObject pooledObject = iterator.next();

                        //被借出的对象不进行清理
                        if(!unborrowedObjectIdList.contains(pooledObject.getId())){
                            continue;
                        }

                        if (!pooledObject.isAlive()) {
                            if(destory!=null){
                                //如果返回 null 则 清理对象, 如果返回为非 null 则刷新对象
                                if(destory.apply(pooledObject)==null){
                                    remove(pooledObject.getId());
                                } else {
                                    pooledObject.refresh();
                                }
                            } else {
                                remove(pooledObject.getId());
                            }
                        }
                    }
                    System.out.println(finalobjectPool.toString());
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
    public class PooledObject{
        private long lastVisiediTime;
        private Object id;
        @NotJSON
        private Object object;
        @NotJSON
        private ObjectPool objectCachedPool;

        public PooledObject(ObjectPool objectCachedPool, Object id, Object object) {
            this.objectCachedPool = objectCachedPool;
            this.lastVisiediTime = System.currentTimeMillis();
            this.id = id;
            this.object = object;
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
        public Object getObject() {
            if(objectCachedPool.isAutoRefreshOnGet()) {
                refresh();
            }
            return object;
        }

        /**
         * 设置设置对象
         * @param object 池中的对象
         */
        public void setObject(Object object) {
            this.object = object;
        }

        /**
         * 缓存的 id
         * @return 缓存的 id
         */
        public Object getId() {
            return id;
        }

        /**
         * 判断对象是否存活
         * @return true: 对象存活, false: 对象超时
         */
        public boolean isAlive(){
            if(objectCachedPool.aliveTime<=0){
                return false;
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
        return "{Total:" + objects.size() + ", unborrow:" + unborrowedObjectIdList.size()+"}";
    }
}

