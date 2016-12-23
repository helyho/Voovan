package org.voovan.tools;

import org.voovan.tools.log.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 对象池
 *
 * @author helyho
 * <p>
 * Vestful Framework.
 * WebSite: https://github.com/helyho/Vestful
 * Licence: Apache v2 License
 */
public class ObjectPool {

    private Map<Integer,PooledObject> objects;
    private Timer timer;
    private long aliveTime = 5;
    private boolean autoRefreshOnGet = true;
    private AtomicInteger objectId = new AtomicInteger(0);


    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,单位:秒
     * @param autoRefreshOnGet 获取对象时刷新对象存活时间
     */
    public ObjectPool(long aliveTime,boolean autoRefreshOnGet){
        objects = new Hashtable<Integer,PooledObject>();
        this.aliveTime = aliveTime;
        timer = new Timer("VOOVAN@Object_Pool_Timer");
        this.autoRefreshOnGet = autoRefreshOnGet;
        removeDeadObject();
    }


    /**
     * 构造一个对象池
     * @param aliveTime 对象存活时间,单位:秒
     */
    public ObjectPool(long aliveTime){
        objects = new Hashtable<Integer,PooledObject>();
        this.aliveTime = aliveTime;
        timer = new Timer("VOOVAN@Object_Pool_Timer");
        removeDeadObject();
    }

    /**
     * 构造一个对象池,默认对象存活事件 5 s
     * @param autoRefreshOnGet 获取对象时刷新对象存活时间
     */
    public ObjectPool(boolean autoRefreshOnGet){
        objects = new Hashtable<Integer,PooledObject>();
        timer = new Timer("VOOVAN@Object_Pool_Timer");
        this.autoRefreshOnGet = autoRefreshOnGet;
        removeDeadObject();
    }

    /**
     * 构造一个对象池,默认对象存活事件 5 s
     */
    public ObjectPool(){
        objects = new Hashtable<Integer,PooledObject>();
        timer = new Timer("VOOVAN@Object_Pool_Timer");
        removeDeadObject();
    }

    /**
     * 生成ObjectId
     * @return 生成的ObjectId
     */
    private int getObjectId(){
        objectId.getAndIncrement();
        return objectId.get();
    }


    /**
     * 是否获取对象时刷新对象存活试驾
     * @return
     */
    public boolean isAutoRefreshOnGet(){
        return autoRefreshOnGet;
    }

    /**
     * 获取池中的对象
     * @param id 对象的 hash 值
     * @return 池中的对象
     */
    public Object get(Integer id){
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
     * @return 对象的 hash 值 ,如果返回 0 ,则增加的是 null 值
     */
    public int add(Object obj){
        if(obj == null){
            return 0;
        }
        int id = getObjectId();
        objects.put(id, new PooledObject(this, id, obj));
        return id;
    }

    /**
     * 判断池中是否存在对象
     * @param id 对象的 hash 值
     * @return true: 存在, false: 不存在
     */
    public boolean contains(Integer id){
        return objects.containsKey(id);
    }

    /**
     * 移除池中的对象
     * @param id 对象的 hash 值
     */
    public void remove(Integer id){
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

    public void removeDeadObject(){
        TimerTask aliveTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    synchronized (objects){
                        for (PooledObject pooledObject : objects.values().toArray(new PooledObject[]{})) {
                            if (!pooledObject.isAlive()) {
                                remove(pooledObject.getId());
                            }
                        }
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
        timer.schedule(aliveTask,1,100);
    }

    /**
     * 池中缓存的对象模型
     */
    private class PooledObject{
        private long createTime;
        private int id;
        private Object object;
        private ObjectPool objectPool;

        public PooledObject(ObjectPool objectPool,int id,Object object) {
            this.objectPool = objectPool;
            this.createTime = System.currentTimeMillis();
            this.id = id;
            this.object = object;
        }

        /**
         * 刷新对象
         */
        public void refresh() {
            createTime = System.currentTimeMillis();
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


        public int getId() {
            return id;
        }

        /**
         * 判断对象是否存活
         * @return
         */
        public boolean isAlive(){
            long currentAliveTime = System.currentTimeMillis() - createTime;
            if (currentAliveTime >= objectPool.aliveTime*1000){
                return false;
            }else{
                return true;
            }
        }
    }
}

