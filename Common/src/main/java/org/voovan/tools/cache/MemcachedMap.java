package org.voovan.tools.cache;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.command.BinaryCommandFactory;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.voovan.tools.log.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.voovan.tools.cache.CacheStatic.defaultPoolSize;

/**
 * 基于 Memcached 的 Map 实现
 *      简单实现,key 和 value 都是 String 类型
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class MemcachedMap<String, V> implements Map<String, V> , Closeable {
    private MemcachedClientBuilder memcachedClientBuilder;
    private MemcachedClient memcachedClient = null;
    private Function<String, V> buildFunction = null;

    /**
     * 构造函数
     * @param host        Memcached 服务地址
     * @param port        Memcached 服务端口
     * @param timeout     Memcached 超时时间
     * @param poolSize    Memcached 服务密码
     */
    public MemcachedMap(String host, int port, int timeout, int poolSize){
        super();
        if(memcachedClientBuilder == null) {
            try {

                if (poolSize == 0) {
                    poolSize = defaultPoolSize();
                }

                memcachedClientBuilder = new XMemcachedClientBuilder(
                        AddrUtil.getAddresses(host + ":" + port));
                memcachedClientBuilder.setFailureMode(true);
                memcachedClientBuilder.setCommandFactory(new BinaryCommandFactory());
                memcachedClientBuilder.setConnectionPoolSize(poolSize);
                memcachedClientBuilder.setConnectTimeout(timeout);
            }catch (Exception e){
                Logger.error("Read ./classes/Memcached.properties error");
            }
        }
    }

    /**
     * 构造函数
     * @param memcachedClientBuilder Memcached 连接池对象
     */
    public MemcachedMap(MemcachedClientBuilder memcachedClientBuilder){
        this.memcachedClientBuilder = memcachedClientBuilder;
    }

    /**
     * 构造函数
     */
    public MemcachedMap(){
        this.memcachedClientBuilder = CacheStatic.getMemcachedPool();
    }

    /**
     * 获取Memcached连接
     * @return Memcached连接
     */
    private MemcachedClient getMemcachedClient(){
        if(memcachedClient == null || memcachedClient.isShutdown()) {
            try {
                memcachedClient = memcachedClientBuilder.build();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return memcachedClient;
    }

    public void build(Function<String, V> buildFunction){
        this.buildFunction = buildFunction;
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            return memcachedClient.get(key.toString())!=null;
        }catch (Exception e){
            Logger.error(e);
            return false;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V get(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            String result = memcachedClient.get(key.toString());

            //如果不存在则重读
            if(result==null){
                if(buildFunction!=null) {
                    synchronized (buildFunction) {
                        V value = buildFunction.apply((String) key);
                        put((String) key, value);
                        return value;
                    }
                }
            }
        }catch (Exception e){
            Logger.error(e);
        }

        return null;
    }


    @Override
    public V put(String key, V value) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            if(memcachedClient.set((java.lang.String) key, 0, value)) {
                return (V)value;
            }else{
                return null;
            }
        }catch (Exception e){
            Logger.error(e);
            return null;
        }
    }

    /**
     * 向 memcached 中放置字符串数据
     * @param key key 名称
     * @param value 数据
     * @param expire 超时事件
     * @return true: 成功, false:失败
     */
    public boolean put(String key, Object value, int expire) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            return memcachedClient.set((java.lang.String) key, expire, value);
        }catch (Exception e){
            Logger.error(e);
            return false;
        }
    }

    /**
     * 像 memcached 中放置对象数据
     *      不管数据存在不存在都会将目前设置的数据存储的 memcached，但不等待返回确认
     * @param key key 名称
     * @param value 数据
     * @param expire 超时时间
     * @return true: 成功, false:失败
     */
    public Boolean putWithNoReply(String key, Object value, int expire) {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            memcachedClient.setWithNoReply((java.lang.String) key, expire, value);
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
        return true;
    }

    @Override
    public V remove(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            V value = memcachedClient.get(key.toString());
            memcachedClient.delete(key.toString());
            return value;
        }catch (Exception e){
            Logger.error(e);
            return null;
        }
    }

    /**
     * 删除指定的 key,不等待返回
     * @param key 键
     */
    public void removeWithNoReply(Object key) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            String value = memcachedClient.get(key.toString());
            memcachedClient.deleteWithNoReply(key.toString());
        }catch (Exception e){
            Logger.error(e);
        }
    }

    @Override
    public void putAll(Map map) {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            for (Object obj : map.entrySet()) {
                Entry entry = (Entry) obj;
                memcachedClient.set(entry.getKey().toString(), 0,entry.getValue().toString());
            }
        }catch (Exception e){
            Logger.error(e);
        }
    }

    @Override
    public void clear() {
        MemcachedClient memcachedClient = getMemcachedClient();
        try{
            memcachedClient.flushAll();
        }catch (Exception e){
            Logger.error(e);
        }
    }

    /**
     * 原子减少操作
     * @param key  key 名称
     * @param value 值
     * @return 自增后的结果
     */
    public long decr(String key, long value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.decr((java.lang.String) key, value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 原子减少操作
     * @param key  key 名称
     * @param value 值
     * @param initValue 默认值
     * @return 自增后的结果
     */
    public long decr(String key, long value , long initValue){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.decr((java.lang.String) key, value , initValue);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 原子增加操作
     * @param key  key 名称
     * @param value 值
     * @return 自增后的结果
     */
    public long incr(String key, long value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.incr((java.lang.String) key, value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 原子增加操作
     * @param key  key 名称
     * @param value 值
     * @param initValue 默认值
     * @return 自增后的结果
     */
    public long incr(String key, long value , long initValue){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.incr((java.lang.String) key, value , initValue);
        } catch (Exception e) {
            Logger.error(e);
        }
        return 0;
    }

    /**
     * 向指定的 key 尾部追加数据
     * @param key key 名称
     * @param value 数据
     * @return true: 成功, false: 失败
     */
    public boolean append(String key, V value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.append((java.lang.String) key, value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return false;
    }

    /**
     * 向指定的 key 头部追加数据
     * @param key key 名称
     * @param value 数据
     * @return true: 成功, false: 失败
     */
    public boolean prepend(String key, V value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return memcachedClient.prepend((java.lang.String) key, value);
        } catch (Exception e) {
            Logger.error(e);
        }
        return false;
    }

    /**
     * 向指定的 key 尾部追加数据,不等待返回
     * @param key key 名称
     * @param value 数据
     */
    public void appendWithNoReply(String key, V value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            memcachedClient.appendWithNoReply((java.lang.String) key, value);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    /**
     * 向指定的 key 头部追加数据,不等待返回
     * @param key key 名称
     * @param value 数据
     */
    public void prependWithNoReply(String key, V value){
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            memcachedClient.prependWithNoReply((java.lang.String) key, value);
        } catch (Exception e) {
            Logger.error(e);
        }
    }

    /**
     * 原子操作
     * @param key key 名称
     * @param value 值
     * @param version 数据版本
     * @return true: 成功, false:失败
     */
    public boolean cas(String key, V value,long version) {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return  memcachedClient.cas((java.lang.String) key, 0, value, version);
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
    }

    /**
     * 原子操作
     * @param key key 名称
     * @param value 值
     * @param time 超时
     * @param version 数据版本
     * @return true: 成功, false:失败
     */
    public boolean cas(String key, V value, int time, long version) {
        MemcachedClient memcachedClient = null;
        try {
            memcachedClient = getMemcachedClient();
            return  memcachedClient.cas((java.lang.String) key, time, value, version);
        } catch (Exception e) {
            Logger.error(e);
            return false;
        }
    }

    @Override
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        memcachedClient.shutdown();
    }
}
