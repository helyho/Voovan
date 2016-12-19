package org.voovan.tools;

import org.voovan.tools.log.Logger;

import java.util.*;

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
     * 是否获取对象时刷新对象存活试驾
     * @return
     */
    public boolean isAutoRefreshOnGet(){
        return autoRefreshOnGet;
    }

    /**
     * 获取池中的对象
     * @param hashCode 对象的 hash 值
     * @return 池中的对象
     */
    public Object get(Integer hashCode){
        PooledObject pooledObject = objects.get(hashCode);
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
        objects.put(obj.hashCode(),new PooledObject(this, obj));
        return obj.hashCode();
    }

    /**
     * 移除池中的对象
     * @param hashCode 对象的 hash 值
     */
    public void remove(Integer hashCode){
        objects.remove(hashCode);
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
                                remove(pooledObject.getObject().hashCode());
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
        private Object object;
        private ObjectPool objectPool;

        public PooledObject(ObjectPool objectPool,Object object) {
            this.objectPool = objectPool;
            this.createTime = System.currentTimeMillis();
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

