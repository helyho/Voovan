package org.voovan.tools.cache;

import org.voovan.Global;
import org.voovan.tools.TString;
import org.voovan.tools.hashwheeltimer.HashWheelTask;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

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
public class ObjectCachedPool {

    private Map<String,PooledObject> objects;
    private long aliveTime = 0;
    private boolean autoRefreshOnGet = true;

    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,小于等于0时为一直存活,单位:秒
     * @param autoRefreshOnGet 获取对象时刷新对象存活时间
     */
    public ObjectCachedPool(long aliveTime, boolean autoRefreshOnGet){
        objects = new ConcurrentSkipListMap<String,PooledObject>();
        this.aliveTime = aliveTime;
        this.autoRefreshOnGet = autoRefreshOnGet;
        removeDeadObject();
    }

    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,单位:秒
     */
    public ObjectCachedPool(long aliveTime){
        objects = new ConcurrentHashMap<String,PooledObject>();
        this.aliveTime = aliveTime;
        removeDeadObject();
    }

    /**
     * 构造一个对象池,默认对象存活事件 5 s
     */
    public ObjectCachedPool(){
        objects = new ConcurrentHashMap<String,PooledObject>();
        removeDeadObject();
    }

    /**
     * 设置对象池的对象存活时间
     * @param aliveTime 对象存活时间,单位:秒
     */
    public void setAliveTime(long aliveTime) {
        this.aliveTime = aliveTime;
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
    public Object get(String id){
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
    public String add(Object obj){
        if(obj == null){
            return null;
        }
        String id = genObjectId();
        objects.put(id, new PooledObject(this, id, obj));
        return id;
    }

    /**
     * 判断池中是否存在对象
     * @param id 对象的 hash 值
     * @return true: 存在, false: 不存在
     */
    public boolean contains(String id){
        return objects.containsKey(id);
    }

    /**
     * 移除池中的对象
     * @param id 对象的 hash 值
     */
    public void remove(String id){
        objects.remove(id);
    }

    /**
     * 获取当前对象池的大小
     * @return 对象池的大小
     */
    public int size(){
        return objects.size();
    }

    /**
     * 清理池中所有的对象
     */
    public void clear(){
        objects.clear();
    }

    /**
     * 借出这个对象
     * @return 借出的对象的 ID
     */
    public String borrow(){
        Iterator<PooledObject> iterator = objects.values().iterator();
        while(iterator.hasNext()){
            PooledObject object = iterator.next();
            String objectId = object.borrow();
            if(objectId!=null){
                return objectId;
            }
        }

        return null;
    }

    /**
     * 归还借出的对象
     */
    public void release(String objectId){
        objects.get(objectId).release();
    }

    private void removeDeadObject(){
        Global.getHashWheelTimer().addTask(new HashWheelTask() {
            @Override
            public void run() {
                try {
                    for (PooledObject pooledObject : objects.values().toArray(new PooledObject[]{})) {
                        if (!pooledObject.isAlive()) {
                            remove(pooledObject.getId());
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        },1, true);
    }

    /**
     * 池中缓存的对象模型
     */
    private class PooledObject{
        private long lastVisiediTime;
        private String id;
        private Object object;
        private ObjectCachedPool objectPool;
        private volatile boolean isBorrowed = false;

        public PooledObject(ObjectCachedPool objectPool, String id, Object object) {
            this.objectPool = objectPool;
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
         * @return
         */
        public Object getObject() {
            if(objectPool.isAutoRefreshOnGet()) {
                refresh();
            }
            return object;
        }

        /**
         * 设置设置对象
         * @param object
         */
        public void setObject(Object object) {
            this.object = object;
        }

        /**
         * 借出这个对象
         * @return 借出的对象的 ID
         */
        public String borrow(){
            synchronized (this) {
                if (!isBorrowed) {
                    isBorrowed = true;
                    return id;
                } else {
                    return null;
                }
            }
        }

        /**
         * 归还借出的对象
         */
        public void release(){
            synchronized (this) {
                if (isBorrowed) {
                    isBorrowed = false;
                }
            }
        }

        public String getId() {
            return id;
        }

        /**
         * 判断对象是否存活
         * @return
         */
        public boolean isAlive(){
            if(objectPool.aliveTime<=0){
                return true;
            }

            long currentAliveTime = System.currentTimeMillis() - lastVisiediTime;
            if (currentAliveTime >= objectPool.aliveTime*1000){
                return false;
            }else{
                return true;
            }
        }
    }
}

